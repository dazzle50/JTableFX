/**************************************************************************
 *  Copyright (C) 2023 by Richard Crook                                   *
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
import java.util.HashSet;
import java.util.TreeSet;

import rjc.table.signal.ISignal;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************************** Table view selected cells/areas ********************************/
/*************************************************************************************************/

public class CellSelection implements ISignal
{
  final static private int     INVALID   = TableAxis.INVALID;
  final static private int     FIRSTCELL = TableAxis.FIRSTCELL;
  final static private int     AFTER     = TableAxis.AFTER;

  private TableView            m_view;
  private ArrayList<Selection> m_selected;

  /***************************************** constructor *****************************************/
  public CellSelection( TableView view )
  {
    // construct selection model
    m_view = view;
    m_selected = new ArrayList<>();
  }

  /******************************************** clear ********************************************/
  public void clear()
  {
    // remove all selected areas
    if ( m_selected.isEmpty() )
      return;

    m_selected.clear();
    signal( m_selected.size() ); // signal selection model has been cleared
  }

  /****************************************** selectAll ******************************************/
  public void selectAll()
  {
    // select entire table
    m_selected.clear(); // direct clear to avoid clear signal before add signal
    select( FIRSTCELL, FIRSTCELL, AFTER, AFTER );
    m_view.getFocusCell().setPosition( FIRSTCELL, FIRSTCELL );
    m_view.getSelectCell().setPosition( AFTER, AFTER );
  }

  /******************************************* select ********************************************/
  public void select( int columnIndex1, int rowIndex1, int columnIndex2, int rowIndex2 )
  {
    // add new selected area to table selected
    Selection newArea = new Selection( columnIndex1, rowIndex1, columnIndex2, rowIndex2 );
    m_selected.add( newArea );
    signal( m_selected.size() );
  }

  /******************************************* select ********************************************/
  public void select()
  {
    // select new area based on focus & select cell positions
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    select( focus.getColumn(), focus.getRow(), select.getColumn(), select.getRow() );
  }

  /**************************************** selectColumns ****************************************/
  public void selectColumns( HashSet<Integer> columns )
  {
    // select specified columns
    var orderedColumns = new TreeSet<Integer>( columns );
    int startColumn = INVALID;
    int endColumn = INVALID;

    for ( int column : orderedColumns )
    {
      if ( startColumn == INVALID )
      {
        startColumn = column;
        endColumn = column;
      }
      else if ( column == endColumn + 1 )
        endColumn = column;
      else
      {
        Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
        m_selected.add( newArea );
        startColumn = column;
        endColumn = column;
      }
    }

    if ( startColumn != INVALID )
    {
      Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /***************************************** selectRows ******************************************/
  public void selectRows( HashSet<Integer> rows )
  {
    // select specified rows
    var orderedRows = new TreeSet<Integer>( rows );
    int startRow = INVALID;
    int endRow = INVALID;

    for ( int row : orderedRows )
    {
      if ( startRow == INVALID )
      {
        startRow = row;
        endRow = row;
      }
      else if ( row == endRow + 1 )
        endRow = row;
      else
      {
        Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
        m_selected.add( newArea );
        startRow = row;
        endRow = row;
      }
    }

    if ( startRow != INVALID )
    {
      Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /******************************************* update ********************************************/
  public void update()
  {
    // update last selected area
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    if ( m_selected.isEmpty() )
      select();
    Selection last = m_selected.get( m_selected.size() - 1 );

    // if selecting columns or rows, then start selecting from first cell
    int focusColumn = select.isColumnAfter() ? FIRSTCELL : focus.getColumn();
    int focusRow = select.isRowAfter() ? FIRSTCELL : focus.getRow();
    last.set( focusColumn, focusRow, select.getColumn(), select.getRow() );
    signal( m_selected.size() );
  }

  /*************************************** isCellSelected ****************************************/
  public boolean isCellSelected( int columnIndex, int rowIndex )
  {
    // return true if specified cell is in a selected area
    for ( var area : m_selected )
      if ( area.isCellSelected( columnIndex, rowIndex ) )
        return true;

    return false;
  }

  /************************************** isColumnSelected ***************************************/
  public boolean isColumnSelected( int columnIndex )
  {
    // return true if all visible cells in specified column are selected
    TableAxis axis = m_view.getCanvas().getRowsAxis();
    int top = axis.getFirstVisible();
    int bottom = axis.getLastVisible();

    for ( int rowIndex = top; rowIndex <= bottom; rowIndex++ )
      rows: if ( axis.isIndexVisible( rowIndex ) )
      {
        for ( var area : m_selected )
          if ( area.isCellSelected( columnIndex, rowIndex ) )
          {
            rowIndex = area.r2;
            break rows;
          }
        return false;
      }

    return true;
  }

  /**************************************** isRowSelected ****************************************/
  public boolean isRowSelected( int rowIndex )
  {
    // return true if all visible cells in specified row are selected
    TableAxis axis = m_view.getCanvas().getColumnsAxis();
    int left = axis.getFirstVisible();
    int right = axis.getLastVisible();

    for ( int columnIndex = left; columnIndex <= right; columnIndex++ )
      columns: if ( axis.isIndexVisible( columnIndex ) )
      {
        for ( var area : m_selected )
          if ( area.isCellSelected( columnIndex, rowIndex ) )
          {
            columnIndex = area.c2;
            break columns;
          }
        return false;
      }

    return true;
  }

  /************************************* hasColumnSelection **************************************/
  public boolean hasColumnSelection( int columnIndex )
  {
    // return true if specified column has any selection
    for ( var area : m_selected )
      if ( columnIndex >= area.c1 && columnIndex <= area.c2 )
        return true;

    return false;
  }

  /*************************************** hasRowSelection ***************************************/
  public boolean hasRowSelection( int rowIndex )
  {
    // return true if specified row has any selection
    for ( var area : m_selected )
      if ( rowIndex >= area.r1 && rowIndex <= area.r2 )
        return true;

    return false;
  }

  /************************************* getSelectedColumns **************************************/
  public HashSet<Integer> getSelectedColumns()
  {
    // return return list of selected columns, null = all, empty-set = none
    var columns = new HashSet<Integer>();
    int first = m_view.getCanvas().getColumnsAxis().getFirstVisible();
    int last = m_view.getCanvas().getColumnsAxis().getLastVisible();
    int top = m_view.getCanvas().getRowsAxis().getFirstVisible();
    int bottom = m_view.getCanvas().getRowsAxis().getLastVisible();

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

  /*************************************** getSelectedRows ***************************************/
  public HashSet<Integer> getSelectedRows()
  {
    // return return list of selected rows, null = all, empty-set = none
    var rows = new HashSet<Integer>();
    int first = m_view.getCanvas().getColumnsAxis().getFirstVisible();
    int last = m_view.getCanvas().getColumnsAxis().getLastVisible();
    int top = m_view.getCanvas().getRowsAxis().getFirstVisible();
    int bottom = m_view.getCanvas().getRowsAxis().getLastVisible();

    // loop through the selected areas
    for ( var area : m_selected )
    {
      // if whole table selected then return null (= all)
      if ( area.c1 <= first && area.c2 >= last && area.r1 <= top && area.r2 >= bottom )
        return null;

      // if rows selected then add to list
      if ( area.c1 <= first && area.c2 >= last )
      {
        for ( int row = area.r1; row <= area.r2; row++ )
          rows.add( row );
      }
    }

    return rows;
  }

  /****************************************** getAreas *******************************************/
  public ArrayList<int[]> getAreas()
  {
    // return list of selected areas - used by CanvasOverlay to draw the highlighting
    int maxColumn = m_view.getCanvas().getColumnsAxis().getCount() - 1;
    int maxRow = m_view.getCanvas().getRowsAxis().getCount() - 1;
    var areas = new ArrayList<int[]>();

    // construct the list
    for ( Selection selected : m_selected )
    {
      int[] area = { selected.c1, selected.r1, Math.min( selected.c2, maxColumn ), Math.min( selected.r2, maxRow ) };
      areas.add( area );
    }

    return areas;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // convert to string
    String text = getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) );
    return text + "[selected=" + m_selected + "]";
  }

}