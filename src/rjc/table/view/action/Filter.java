/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.view.action;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import rjc.table.HashSetInt;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Filter table-view columns/rows via undo command ************************/
/*************************************************************************************************/

public class Filter
{
  // maps to hold current index filter counts for each TableView
  private static final Map<TableView, Map<Integer, Integer>> COLUMN_FILTERS = new WeakHashMap<>();
  private static final Map<TableView, Map<Integer, Integer>> ROW_FILTERS    = new WeakHashMap<>();

  /************************************* columnTextContains **************************************/
  public static void columnTextContains( TableView view, int viewIndex, String text, boolean caseSensitive )
  {
    // filter rows where column text contains the specified text (hide those that don't)
    Predicate<String> predicate = caseSensitive ? valueText -> valueText.contains( text )
        : valueText -> ( valueText.toLowerCase( Locale.ROOT ) ).contains( text.toLowerCase( Locale.ROOT ) );

    filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( viewIndex ), predicate );
  }

  /************************************** columnTextStarts ***************************************/
  public static void columnTextStarts( TableView view, int viewIndex, String text, boolean caseSensitive )
  {
    // filter rows where column text starts with the specified text (hide those that don't)
    Predicate<String> predicate = caseSensitive ? valueText -> valueText.startsWith( text )
        : valueText -> ( valueText.toLowerCase( Locale.ROOT ) ).startsWith( text.toLowerCase( Locale.ROOT ) );

    filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( viewIndex ), predicate );
  }

  /************************************** columnTextRegex ****************************************/
  public static void columnTextRegex( TableView view, int viewIndex, String regex, boolean caseSensitive )
  {
    // filter rows where column text matches the regex pattern (hide those that don't)
    try
    {
      var pattern = caseSensitive ? Pattern.compile( regex ) : Pattern.compile( regex, Pattern.CASE_INSENSITIVE );

      filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( viewIndex ),
          valueText -> pattern.matcher( valueText ).find() );
    }
    catch ( Exception exception )
    {
      Utils.trace( view, viewIndex, regex, caseSensitive, exception );
      view.getStatus().update( Level.ERROR, "Invalid regex pattern: " + regex );
      return;
    }
  }

  /*************************************** rowTextContains ***************************************/
  public static void rowTextContains( TableView view, int viewIndex, String text, boolean caseSensitive )
  {
    // filter columns where row text contains the specified text (hide those that don't)
    Predicate<String> predicate = caseSensitive ? valueText -> valueText.contains( text )
        : valueText -> ( valueText.toLowerCase( Locale.ROOT ) ).contains( text.toLowerCase( Locale.ROOT ) );

    filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( viewIndex ), predicate );
  }

  /**************************************** rowTextStarts ****************************************/
  public static void rowTextStarts( TableView view, int viewIndex, String text, boolean caseSensitive )
  {
    // filter columns where row text starts with the specified text (hide those that don't)
    Predicate<String> predicate = caseSensitive ? valueText -> valueText.startsWith( text )
        : valueText -> ( valueText.toLowerCase( Locale.ROOT ) ).startsWith( text.toLowerCase( Locale.ROOT ) );

    filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( viewIndex ), predicate );
  }

  /**************************************** rowTextRegex *****************************************/
  public static void rowTextRegex( TableView view, int viewIndex, String regex, boolean caseSensitive )
  {
    // filter columns where row text matches the regex pattern (hide those that don't)
    try
    {
      var pattern = caseSensitive ? Pattern.compile( regex ) : Pattern.compile( regex, Pattern.CASE_INSENSITIVE );

      filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( viewIndex ),
          valueText -> pattern.matcher( valueText ).find() );
    }
    catch ( Exception exception )
    {
      Utils.trace( view, viewIndex, regex, caseSensitive, exception );
      view.getStatus().update( Level.ERROR, "Invalid regex pattern: " + regex );
      return;
    }
  }

  /****************************************** filterAxis *****************************************/
  private static void filterAxis( TableView view, TableAxis filterAxis, int filterDataIndex, Predicate<String> filter )
  {
    // collect indexes that don't match the filter predicate
    var indexesToHide = new HashSetInt();
    boolean isFilteringRows = filterAxis == view.getRowsAxis();
    int valuesChecked = 0;

    // iterate through all visible indexes to check for text match
    int checkViewIndex = filterAxis.getFirstVisible();
    int previousViewIndex;

    do
    {
      // get cell value based on whether filtering rows or columns
      int checkDataIndex = filterAxis.getDataIndex( checkViewIndex );
      Object cellValue = isFilteringRows ? view.getData().getValue( filterDataIndex, checkDataIndex )
          : view.getData().getValue( checkDataIndex, filterDataIndex );
      String cellText = cellValue == null ? "" : cellValue.toString();

      // add index to hide set if filter test fails
      if ( !filter.test( cellText ) )
        indexesToHide.add( checkViewIndex );

      // advance to next visible index
      valuesChecked++;
      previousViewIndex = checkViewIndex;
      checkViewIndex = filterAxis.getNextVisible( checkViewIndex );
    }
    while ( checkViewIndex != previousViewIndex );

    // update status with number of records found
    int found = valuesChecked - indexesToHide.size();
    view.getStatus().update( Level.INFO, found + " of " + valuesChecked + " records found" );

    // execute hide command via undo stack
    var hideCommand = new CommandHideIndexes( view, filterAxis, indexesToHide );
    view.getUndoStack().push( hideCommand );
  }

  /*************************************** isColumnFiltered **************************************/
  public static boolean isColumnFiltered( TableView view, int dataColumn )
  {
    // return true if specified data column in view is currently filtered
    return COLUMN_FILTERS.getOrDefault( view, Map.of() ).containsKey( dataColumn );
  }

  /**************************************** isRowFiltered ****************************************/
  public static boolean isRowFiltered( TableView m_view, int dataRow )
  {
    // return true if specified data row in view is currently filtered
    return ROW_FILTERS.getOrDefault( m_view, Map.of() ).containsKey( dataRow );
  }

  /************************************* incrementColumnFilter ***********************************/
  public static void incrementColumnFilter( TableView view, int dataColumn )
  {
    // increment filter count for specified data column in view
    COLUMN_FILTERS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).merge( dataColumn, 1, Integer::sum );
  }

  /************************************* decrementColumnFilter ***********************************/
  public static void decrementColumnFilter( TableView view, int dataColumn )
  {
    // decrement filter count for specified data column in view
    Map<Integer, Integer> filters = COLUMN_FILTERS.get( view );
    if ( filters != null )
    {
      filters.computeIfPresent( dataColumn, ( k, v ) -> ( v > 1 ) ? v - 1 : null );
      if ( filters.isEmpty() )
        COLUMN_FILTERS.remove( view );
    }
  }

  /************************************** incrementRowFilter *************************************/
  public static void incrementRowFilter( TableView view, int dataRow )
  {
    // increment filter count for specified data row in view
    ROW_FILTERS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).merge( dataRow, 1, Integer::sum );
  }

  /************************************** decrementRowFilter *************************************/
  public static void decrementRowFilter( TableView view, int dataRow )
  {
    // decrement filter count for specified data row in view
    Map<Integer, Integer> filters = ROW_FILTERS.get( view );
    if ( filters != null )
    {
      filters.computeIfPresent( dataRow, ( k, v ) -> ( v > 1 ) ? v - 1 : null );
      if ( filters.isEmpty() )
        ROW_FILTERS.remove( view );
    }
  }

}