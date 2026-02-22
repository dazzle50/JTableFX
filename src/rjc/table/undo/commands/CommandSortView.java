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

package rjc.table.undo.commands;

import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.action.Sort;
import rjc.table.view.action.Sort.SortType;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/***************** UndoCommand for sorting table-view by specified column or row *****************/
/*************************************************************************************************/

/**
 * Undoable command for sorting a table view by reordering axis mapping.
 * <p>
 * Reorders view presentation without modifying underlying data. Maintains
 * the mapping between view positions and data indices, enabling independent
 * sorting of what users see versus actual data storage order. Sort state is
 * recorded in {@link Sort#columnSort} or {@link Sort#rowSort} so the overlay
 * can display sort-direction indicators.
 *
 * @see IUndoCommand
 * @see TableView
 * @see TableAxis
 */
public class CommandSortView implements IUndoCommand
{
  private final TableView m_view;          // table view
  private final TableAxis m_axis;          // axis being reordered
  private final int[]     m_oldOrder;      // data-indices order before sort
  private final int[]     m_newOrder;      // data-indices order after sort
  private final int       m_sortDataIndex; // data index of the column or row used as sort key
  private final SortType  m_sortType;      // sort direction
  private SortType        m_previousSort;  // map value present before last redo(); NOTSORTED = absent
  private String          m_text;          // lazily constructed description for undo/redo UI

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a view sorting command.
   * <p>
   * Converts view-order indices to data-order indices for tracking original
   * state, then immediately applies the sorted mapping via {@link #redo()}.
   *
   * @param view            the table view to reorder
   * @param axis            the axis (rows or columns) being sorted
   * @param beforeViewOrder array of view-indices representing current view positions
   * @param afterDataOrder  array of data-indices representing desired sorted order
   * @param sortDataIndex   data index of the column or row used as the sort key
   * @param sortType        the sort direction ({@code ASCENDING} or {@code DESCENDING})
   */
  public CommandSortView( TableView view, TableAxis axis, int[] beforeViewOrder, int[] afterDataOrder,
      int sortDataIndex, SortType sortType )
  {
    // convert view order to data order for before-sort snapshot
    m_oldOrder = new int[beforeViewOrder.length];
    for ( int i = 0; i < beforeViewOrder.length; i++ )
      m_oldOrder[i] = axis.getDataIndex( beforeViewOrder[i] );

    m_newOrder = afterDataOrder;
    m_axis = axis;
    m_view = view;
    m_sortDataIndex = sortDataIndex;
    m_sortType = sortType;
    redo();
  }

  /********************************************** redo ********************************************/
  /**
   * Executes the sort operation and records sort state for overlay highlighting.
   * Captures the pre-existing map value so {@link #undo()} can restore it exactly.
   */
  @Override
  public void redo()
  {
    var map = m_axis == m_view.getRowsAxis() ? Sort.columnSort : Sort.rowSort;

    // snapshot whatever is in the map before overwriting (NOTSORTED signals absent)
    m_previousSort = map.get( m_view, m_sortDataIndex, SortType.NOTSORTED );

    // apply the post-sorted index mapping and record sort state
    m_axis.setMapping( m_newOrder );
    map.put( m_view, m_sortDataIndex, m_sortType );
    m_view.redraw();
  }

  /********************************************** undo ********************************************/
  /**
   * Reverses the sort operation and restores the previous sort state in the overlay.
   * If no sort state existed before this command, the map entry is removed entirely.
   */
  @Override
  public void undo()
  {
    var map = m_axis == m_view.getRowsAxis() ? Sort.columnSort : Sort.rowSort;

    // restore the before-sort index mapping and restore previous map state
    m_axis.setMapping( m_oldOrder );
    if ( m_previousSort == SortType.NOTSORTED )
      map.remove( m_view, m_sortDataIndex );
    else
      map.put( m_view, m_sortDataIndex, m_previousSort );
    m_view.redraw();
  }

  /********************************************* text *********************************************/
  /**
   * Returns a description of this command for undo/redo UI.
   * <p>
   * Lazily constructs the description from the sort-key header and direction arrow.
   * Prepends the view ID when one is set. Arrow convention: column sorts use
   * {@code ↓} for ascending, row sorts use {@code ↑}.
   *
   * @return command description
   */
  @Override
  public String text()
  {
    if ( m_text == null )
    {
      // determine whether the sort key is a column (rows axis sorted) or row (columns axis sorted)
      boolean sortedByColumn = m_axis == m_view.getRowsAxis();

      // retrieve header label for the sort-key column or row
      String header = sortedByColumn ? m_view.getData().getValue( m_sortDataIndex, TableData.HEADER ).toString()
          : m_view.getData().getValue( TableData.HEADER, m_sortDataIndex ).toString();

      // arrow direction: column sorts use ↓ for ascending, row sorts use ↑ for ascending
      boolean ascending = m_sortType == SortType.ASCENDING;
      String arrow = sortedByColumn ? ( ascending ? "▲" : "▼" ) : ( ascending ? "◀" : "▶" );

      m_text = "Sorted " + ( sortedByColumn ? "column " : "row " ) + header + " " + arrow;

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }

}