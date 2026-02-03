/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

package rjc.table.view.cell;

import java.util.ArrayList;

import rjc.table.HashSetInt;
import rjc.table.Utils;
import rjc.table.signal.ISignal;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Manages selected cells and areas on table view *************************/
/*************************************************************************************************/

/**
 * Manages the selection model for a table view, tracking which cells, rows, and columns are
 * currently selected. Supports multiple non-contiguous selection areas and provides methods to
 * query and modify the selection state. Implements ISignal to notify observers when the
 * selection changes.
 */
public class CellSelection implements ISignal
{
  final static private int     INVALID   = TableAxis.INVALID;
  final static private int     FIRSTCELL = TableAxis.FIRSTCELL;
  final static private int     AFTER     = TableAxis.AFTER;

  private TableView            m_view;
  private ArrayList<Selection> m_selected;

  /***************************************** constructor *****************************************/
  /**
   * Constructs a new cell selection model for the specified table view.
   *
   * @param view the table view this selection model is associated with
   */
  public CellSelection( TableView view )
  {
    // construct selection model
    m_view = view;
    m_selected = new ArrayList<>();
  }

  /******************************************** clear ********************************************/
  /**
   * Removes all selected areas from the selection model. If no areas are currently selected,
   * this method has no effect. Signals that the selection has been cleared.
   */
  public void clear()
  {
    // remove all selected areas
    if ( m_selected.isEmpty() )
      return;

    m_selected.clear();
    signal( m_selected.size() ); // signal selection model has been cleared
  }

  /****************************************** selectAll ******************************************/
  /**
   * Selects the entire table by creating a selection area from the first cell to after the last
   * cell. Also positions the focus cell at the first cell and the select cell after the last cell.
   */
  public void selectAll()
  {
    // select entire table
    m_selected.clear(); // direct clear to avoid clear signal before add signal
    select( FIRSTCELL, FIRSTCELL, AFTER, AFTER );
    m_view.getFocusCell().setPosition( FIRSTCELL, FIRSTCELL );
    m_view.getSelectCell().setPosition( AFTER, AFTER );
  }

  /******************************************* select ********************************************/
  /**
   * Adds a new rectangular selection area to the table selection model, defined by two corner
   * positions. The coordinates may be in any order (min/max are calculated internally).
   *
   * @param viewColumn1 the column view index of the first corner
   * @param viewRow1    the row view index of the first corner
   * @param viewColumn2 the column view index of the second corner
   * @param viewRow2    the row view index of the second corner
   */
  public void select( int viewColumn1, int viewRow1, int viewColumn2, int viewRow2 )
  {
    // add new selected area to table selected
    Selection newArea = new Selection( viewColumn1, viewRow1, viewColumn2, viewRow2 );
    m_selected.add( newArea );
    signal( m_selected.size() );
  }

  /******************************************* select ********************************************/
  /**
   * Selects a new rectangular area based on the current positions of the focus cell and the
   * select cell. The area is defined by the rectangle between these two positions.
   */
  public void select()
  {
    // select new area based on focus & select cell positions
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    select( focus.getColumn(), focus.getRow(), select.getColumn(), select.getRow() );
  }

  /**************************************** selectColumns ****************************************/
  /**
   * Selects the specified columns by creating selection areas for contiguous ranges. Non-contiguous
   * columns result in multiple selection areas. Each selection area spans from the first row to
   * after the last row.
   *
   * @param viewColumnsToSelect the set of column view indices to select
   */
  public void selectColumns( HashSetInt viewColumnsToSelect )
  {
    // select specified columns by creating areas for contiguous ranges
    var sortedColumns = viewColumnsToSelect.toSortedArray();
    int startColumn = INVALID;
    int endColumn = INVALID;

    // iterate through sorted columns to identify contiguous ranges
    for ( int columnIndex : sortedColumns )
    {
      if ( startColumn == INVALID )
      {
        // start new range
        startColumn = columnIndex;
        endColumn = columnIndex;
      }
      else if ( columnIndex == endColumn + 1 )
        // extend current range
        endColumn = columnIndex;
      else
      {
        // save current range and start new range
        Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
        m_selected.add( newArea );
        startColumn = columnIndex;
        endColumn = columnIndex;
      }
    }

    // add final range if one exists
    if ( startColumn != INVALID )
    {
      Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /***************************************** selectRows ******************************************/
  /**
   * Selects the specified rows by creating selection areas for contiguous ranges. Non-contiguous
   * rows result in multiple selection areas. Each selection area spans from the first column to
   * after the last column.
   *
   * @param viewRowsToSelect the set of row view indices to select
   */
  public void selectRows( HashSetInt viewRowsToSelect )
  {
    // select specified rows by creating areas for contiguous ranges
    var sortedRows = viewRowsToSelect.toSortedArray();
    int startRow = INVALID;
    int endRow = INVALID;

    // iterate through sorted rows to identify contiguous ranges
    for ( int rowIndex : sortedRows )
    {
      if ( startRow == INVALID )
      {
        // start new range
        startRow = rowIndex;
        endRow = rowIndex;
      }
      else if ( rowIndex == endRow + 1 )
        // extend current range
        endRow = rowIndex;
      else
      {
        // save current range and start new range
        Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
        m_selected.add( newArea );
        startRow = rowIndex;
        endRow = rowIndex;
      }
    }

    // add final range if one exists
    if ( startRow != INVALID )
    {
      Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /******************************************* update ********************************************/
  /**
   * Updates the last selected area to reflect the current positions of the focus and select cells.
   * If no selection exists, creates a new one. When selecting entire columns or rows, adjusts the
   * focus position to start from the first cell.
   */
  public void update()
  {
    // update last selected area to match current focus and select cell positions
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    if ( m_selected.isEmpty() )
      select();
    Selection lastSelectedArea = m_selected.get( m_selected.size() - 1 );

    // if selecting columns or rows, then start selecting from first cell
    int anchorColumn = select.isColumnAfter() ? FIRSTCELL : focus.getColumn();
    int anchorRow = select.isRowAfter() ? FIRSTCELL : focus.getRow();
    lastSelectedArea.set( anchorColumn, anchorRow, select.getColumn(), select.getRow() );
    signal( m_selected.size() );
  }

  /*************************************** isCellSelected ****************************************/
  /**
   * Determines whether the specified cell is contained within any of the currently selected areas.
   *
   * @param viewColumn the column view index to check
   * @param viewRow    the row view index to check
   * @return true if the cell is selected, false otherwise
   */
  public boolean isCellSelected( int viewColumn, int viewRow )
  {
    // return true if specified cell is in a selected area
    for ( var area : m_selected )
      if ( area.isCellSelected( viewColumn, viewRow ) )
        return true;

    return false;
  }

  /************************************** isColumnSelected ***************************************/
  /**
   * Determines whether all visible cells in the specified column are selected. Returns false if
   * the column has no visible cells or if any visible cell in the column is not selected.
   *
   * @param viewColumn the column view index to check
   * @return true if all visible cells in the column are selected, false otherwise
   */
  public boolean isColumnSelected( int viewColumn )
  {
    // return true if all visible cells in specified column are selected
    TableAxis axis = m_view.getRowsAxis();
    if ( axis.getCount() == 0 )
      return false;

    int top = axis.getFirstVisible();
    int bottom = axis.getLastVisible();

    // check each visible row in this column
    for ( int rowIndex = top; rowIndex <= bottom; rowIndex++ )
      rows: if ( axis.isVisible( rowIndex ) )
      {
        // check if this cell is within any selected area
        for ( var area : m_selected )
          if ( area.isCellSelected( viewColumn, rowIndex ) )
          {
            // skip ahead to end of this selected area's row range
            rowIndex = area.r2;
            break rows;
          }
        // cell is not selected, so column is not fully selected
        return false;
      }

    return true;
  }

  /**************************************** isRowSelected ****************************************/
  /**
   * Determines whether all visible cells in the specified row are selected. Returns false if
   * the row has no visible cells or if any visible cell in the row is not selected.
   *
   * @param viewRow the row view index to check
   * @return true if all visible cells in the row are selected, false otherwise
   */
  public boolean isRowSelected( int viewRow )
  {
    // return true if all visible cells in specified row are selected
    TableAxis axis = m_view.getColumnsAxis();
    if ( axis.getCount() == 0 )
      return false;

    int left = axis.getFirstVisible();
    int right = axis.getLastVisible();

    // check each visible column in this row
    for ( int columnIndex = left; columnIndex <= right; columnIndex++ )
      columns: if ( axis.isVisible( columnIndex ) )
      {
        // check if this cell is within any selected area
        for ( var area : m_selected )
          if ( area.isCellSelected( columnIndex, viewRow ) )
          {
            // skip ahead to end of this selected area's column range
            columnIndex = area.c2;
            break columns;
          }
        // cell is not selected, so row is not fully selected
        return false;
      }

    return true;
  }

  /************************************* hasColumnSelection **************************************/
  /**
   * Determines whether the specified column intersects with any selected area. Returns true if
   * any part of the column is included in a selection, even if not all cells are selected.
   *
   * @param viewColumn the column view index to check
   * @return true if the column has any selection, false otherwise
   */
  public boolean hasColumnSelection( int viewColumn )
  {
    // return true if specified column has any selection
    for ( var area : m_selected )
      if ( viewColumn >= area.c1 && viewColumn <= area.c2 )
        return true;

    return false;
  }

  /*************************************** hasRowSelection ***************************************/
  /**
   * Determines whether the specified row intersects with any selected area. Returns true if
   * any part of the row is included in a selection, even if not all cells are selected.
   *
   * @param viewRow the row view index to check
   * @return true if the row has any selection, false otherwise
   */
  public boolean hasRowSelection( int viewRow )
  {
    // return true if specified row has any selection
    for ( var area : m_selected )
      if ( viewRow >= area.r1 && viewRow <= area.r2 )
        return true;

    return false;
  }

  /************************************* getResizableColumns **************************************/
  /**
   * Returns a set of selected column view indices, excluding those that are not resizable. If all
   * columns are selected, returns null to indicate all columns (filtered for resizability).
   *
   * @return a HashSetInt of resizable selected columns, null if all columns selected, or empty set if none
   */
  public HashSetInt getResizableColumns()
  {
    // return list of selected columns with non-resizable removed
    var selectedColumns = getSelectedColumns();
    if ( selectedColumns != null )
      selectedColumns.removeIf( columnIndex -> !m_view.isColumnResizable( columnIndex ) );

    return selectedColumns;
  }

  /************************************* getSelectedColumns **************************************/
  /**
   * Returns a set of all selected column view indices. Returns null if the entire table is selected
   * (indicating all columns), or an empty set if no columns are selected. A column is considered
   * selected if it spans from the first to last visible row within a selection area.
   *
   * @return a HashSetInt of selected columns, null if all columns, or empty set if none
   */
  public HashSetInt getSelectedColumns()
  {
    // return return list of selected columns, null = all, empty-set = none
    var columns = new HashSetInt();
    int first = m_view.getColumnsAxis().getFirstVisible();
    int last = m_view.getColumnsAxis().getLastVisible();
    int top = m_view.getRowsAxis().getFirstVisible();
    int bottom = m_view.getRowsAxis().getLastVisible();

    // loop through the selected areas
    for ( var area : m_selected )
    {
      // if whole table selected then return null (= all)
      if ( area.c1 <= first && area.c2 >= last && area.r1 <= top && area.r2 >= bottom )
        return null;

      // if columns selected then add to set
      if ( area.r1 <= top && area.r2 >= bottom )
      {
        for ( int column = area.c1; column <= area.c2; column++ )
          columns.add( column );
      }
    }

    return columns;
  }

  /************************************** getResizableRows ***************************************/
  /**
   * Returns a set of selected row view indices, excluding those that are not resizable. If all
   * rows are selected, returns null to indicate all rows (filtered for resizability).
   *
   * @return a HashSetInt of resizable selected rows, null if all rows selected, or empty set if none
   */
  public HashSetInt getResizableRows()
  {
    // return list of selected rows with non-resizable removed
    var selectedRows = getSelectedRows();
    if ( selectedRows != null )
      selectedRows.removeIf( rowIndex -> !m_view.isRowResizable( rowIndex ) );

    return selectedRows;
  }

  /*************************************** getSelectedRows ***************************************/
  /**
   * Returns a set of all selected row view indices. Returns null if the entire table is selected
   * (indicating all rows), or an empty set if no rows are selected. A row is considered
   * selected if it spans from the first to last visible column within a selection area.
   *
   * @return a HashSetInt of selected rows, null if all rows, or empty set if none
   */
  public HashSetInt getSelectedRows()
  {
    // return return list of selected rows, null = all, empty-set = none
    var rows = new HashSetInt();
    int first = m_view.getColumnsAxis().getFirstVisible();
    int last = m_view.getColumnsAxis().getLastVisible();
    int top = m_view.getRowsAxis().getFirstVisible();
    int bottom = m_view.getRowsAxis().getLastVisible();

    // loop through the selected areas
    for ( var area : m_selected )
    {
      // if whole table selected then return null (= all)
      if ( area.c1 <= first && area.c2 >= last && area.r1 <= top && area.r2 >= bottom )
        return null;

      // if rows selected (spanning all columns) then add to list
      if ( area.c1 <= first && area.c2 >= last )
      {
        for ( int row = area.r1; row <= area.r2; row++ )
          rows.add( row );
      }
    }

    return rows;
  }

  /********************************** areAllVisibleRowsSelected **********************************/
  /**
   * Checks if all visible rows are currently selected.
   *
   * @return true if all visible rows are selected, false otherwise
   */
  public boolean areAllVisibleRowsSelected()
  {
    // return true if all visible rows are selected
    int top = m_view.getRowsAxis().getFirstVisible();
    int bottom = m_view.getRowsAxis().getLastVisible();

    // check each visible row, return false as soon as one is not selected
    for ( int row = top; row <= bottom; row++ )
      if ( m_view.getRowsAxis().isVisible( row ) && !isRowSelected( row ) )
        return false;

    // all visible rows are selected
    return true;
  }

  /******************************** areAllVisibleColumnsSelected *********************************/
  /**
   * Checks if all visible columns are currently selected.
   *
   * @return true if all visible columns are selected, false otherwise
   */
  public boolean areAllVisibleColumnsSelected()
  {
    // return true if all visible columns are selected
    int left = m_view.getColumnsAxis().getFirstVisible();
    int right = m_view.getColumnsAxis().getLastVisible();

    // check each visible column, return false as soon as one is not selected
    for ( int col = left; col <= right; col++ )
      if ( m_view.getColumnsAxis().isVisible( col ) && !isColumnSelected( col ) )
        return false;

    // all visible columns are selected
    return true;
  }

  /****************************************** getAreas *******************************************/
  /**
   * Returns a list of all selected areas as integer arrays. Each array contains four elements:
   * [columnStart, rowStart, columnEnd, rowEnd]. The end coordinates are clamped to the maximum
   * valid column and row view indices. This method is used by the canvas overlay to draw selection
   * highlighting.
   *
   * @return an ArrayList of int arrays, each representing a selected area's bounds
   */
  public ArrayList<int[]> getAreas()
  {
    // return list of selected areas - used by CanvasOverlay to draw the highlighting
    int maxColumnIndex = m_view.getColumnsAxis().getCount() - 1;
    int maxRowIndex = m_view.getRowsAxis().getCount() - 1;
    var selectedAreas = new ArrayList<int[]>();

    // construct the list with clamped coordinates
    for ( Selection selectedArea : m_selected )
    {
      int[] areaBounds = { selectedArea.c1, selectedArea.r1, Math.min( selectedArea.c2, maxColumnIndex ),
          Math.min( selectedArea.r2, maxRowIndex ) };
      selectedAreas.add( areaBounds );
    }

    return selectedAreas;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // convert to string
    return Utils.name( this ) + "[selected=" + m_selected + "]";
  }

}