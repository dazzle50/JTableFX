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

import java.util.function.Predicate;

import rjc.table.HashSetInt;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Filter table-view columns/rows via undo command ************************/
/*************************************************************************************************/

public class Filter
{
  /************************************* columnTextContains **************************************/
  public static void columnTextContains( TableView view, int mouseCol, String text )
  {
    // filter rows where column text contains the specified text
    filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( mouseCol ),
        valueText -> valueText.contains( text ) );
  }

  /************************************** columnTextStarts ***************************************/
  public static void columnTextStarts( TableView view, int mouseCol, String text )
  {
    // filter rows where column text starts with the specified text
    filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( mouseCol ),
        valueText -> valueText.startsWith( text ) );
  }

  /************************************** columnTextRegex ****************************************/
  public static void columnTextRegex( TableView view, int mouseCol, String regex )
  {
    // filter rows where column text matches the regex pattern
    var pattern = java.util.regex.Pattern.compile( regex );
    filterAxis( view, view.getRowsAxis(), view.getColumnsAxis().getDataIndex( mouseCol ),
        valueText -> pattern.matcher( valueText ).find() );
  }

  /*************************************** rowTextContains ***************************************/
  public static void rowTextContains( TableView view, int mouseRow, String text )
  {
    // filter columns where row text contains the specified text
    filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( mouseRow ),
        valueText -> valueText.contains( text ) );
  }

  /**************************************** rowTextStarts ****************************************/
  public static void rowTextStarts( TableView view, int mouseRow, String text )
  {
    // filter columns where row text starts with the specified text
    filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( mouseRow ),
        valueText -> valueText.startsWith( text ) );
  }

  /**************************************** rowTextRegex *****************************************/
  public static void rowTextRegex( TableView view, int mouseRow, String regex )
  {
    // filter columns where row text matches the regex pattern
    var pattern = java.util.regex.Pattern.compile( regex );
    filterAxis( view, view.getColumnsAxis(), view.getRowsAxis().getDataIndex( mouseRow ),
        valueText -> pattern.matcher( valueText ).find() );
  }

  /****************************************** filterAxis *****************************************/
  private static void filterAxis( TableView view, TableAxis axisToFilter, int fixedDataIndex,
      Predicate<String> textPredicate )
  {
    // collect indexes that don't match the filter predicate
    var indexesToHide = new HashSetInt();
    boolean isFilteringRows = axisToFilter == view.getRowsAxis();
    int valuesChecked = 0;

    // iterate through all visible indexes to check for text match
    int currentVisibleIndex = axisToFilter.getFirstVisible();
    int previousVisibleIndex;

    do
    {
      // get data index for current visible position
      int currentDataIndex = axisToFilter.getDataIndex( currentVisibleIndex );

      // get cell value based on whether filtering rows or columns
      Object cellValue = isFilteringRows ? view.getData().getValue( fixedDataIndex, currentDataIndex )
          : view.getData().getValue( currentDataIndex, fixedDataIndex );
      String cellText = cellValue == null ? "" : cellValue.toString();

      // add index to hide set if predicate test fails
      if ( !textPredicate.test( cellText ) )
        indexesToHide.add( currentVisibleIndex );

      // advance to next visible index
      valuesChecked++;
      previousVisibleIndex = currentVisibleIndex;
      currentVisibleIndex = axisToFilter.getNextVisible( currentVisibleIndex );
    }
    while ( currentVisibleIndex != previousVisibleIndex );

    // update status with number of records found
    int found = valuesChecked - indexesToHide.size();
    view.getStatus().update( Level.NORMAL, found + " of " + valuesChecked + " records found" );

    // execute hide command via undo stack
    var hideCommand = new CommandHideIndexes( view, axisToFilter, indexesToHide );
    view.getUndoStack().push( hideCommand );
  }
}