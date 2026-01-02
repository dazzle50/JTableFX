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
import java.util.Map;
import java.util.WeakHashMap;

import rjc.table.undo.commands.CommandSortView;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Sort table-view columns/rows via undo command *************************/
/*************************************************************************************************/

public class Sort
{
  // maps to hold current index sort status for each TableView
  private static final Map<TableView, Map<Integer, SortType>> COLUMN_SORTS = new WeakHashMap<>();
  private static final Map<TableView, Map<Integer, SortType>> ROW_SORTS    = new WeakHashMap<>();

  public enum SortType
  {
    ASCENDING, DESCENDING, NOTSORTED
  }

  /*************************************** isColumnSorted ****************************************/
  public static SortType isColumnSorted( TableView view, int dataColumn )
  {
    // return sorted status of specified data column in specified view
    return COLUMN_SORTS.getOrDefault( view, Map.of() ).getOrDefault( dataColumn, SortType.NOTSORTED );
  }

  /***************************************** isRowSorted *****************************************/
  public static SortType isRowSorted( TableView view, int dataRow )
  {
    // return sorted status of specified data row in specified view
    return ROW_SORTS.getOrDefault( view, Map.of() ).getOrDefault( dataRow, SortType.NOTSORTED );
  }

  /************************************** setColumnSorted ****************************************/
  public static void setColumnSorted( TableView view, int dataColumn, SortType status )
  {
    // set sorted status of specified data column in specified view
    COLUMN_SORTS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).put( dataColumn, status );
  }

  /*************************************** setRowSorted ******************************************/
  public static void setRowSorted( TableView view, int dataRow, SortType status )
  {
    // set sorted status of specified data row in specified view
    ROW_SORTS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).put( dataRow, status );
  }

  /*************************************** columnTextSort ****************************************/
  public static boolean columnSort( TableView view, int viewColumn, SortType type )
  {
    // perform the sort via undo-command and add to undostack
    var command = new CommandSortView( view, view.getColumnsAxis(), viewColumn, type );
    return view.getUndoStack().push( command );
  }

  /***************************************** rowTextSort *****************************************/
  public static boolean rowSort( TableView view, int viewRow, SortType type )
  {
    // perform the sort via undo-command and add to undostack
    var command = new CommandSortView( view, view.getRowsAxis(), viewRow, type );
    return view.getUndoStack().push( command );
  }

}
