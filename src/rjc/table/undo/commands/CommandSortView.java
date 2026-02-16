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

import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/***************** UndoCommand for sorting table-view by specified column or row *****************/
/*************************************************************************************************/

/**
 * Undoable command for sorting table view by reordering axis mapping.
 * <p>
 * Reorders view presentation without modifying underlying data. Maintains
 * mapping between view positions and data indices, enabling independent
 * sorting of what users see versus actual data storage order.
 * 
 * @see IUndoCommand
 * @see TableView
 * @see TableAxis
 */
public class CommandSortView implements IUndoCommand
{
  private TableView m_view;     // table view
  private TableAxis m_axis;     // axis being reordered
  private String    m_label;    // label of column/row being sorted
  private int[]     m_oldOrder; // data-indexes order before sort
  private int[]     m_newOrder; // data-indexes order after sort
  private String    m_text;     // text describing command

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
   * @param label           description of the sort column/row for display purposes
   */
  public CommandSortView( TableView view, TableAxis axis, int[] beforeViewOrder, int[] afterDataOrder, String label )
  {
    // convert view order to data order for before-sort
    m_oldOrder = new int[beforeViewOrder.length];
    for ( int i = 0; i < beforeViewOrder.length; i++ )
      m_oldOrder[i] = axis.getDataIndex( beforeViewOrder[i] );

    m_newOrder = afterDataOrder;
    m_label = label;
    m_axis = axis;
    m_view = view;
    redo();
  }

  /********************************************** redo ********************************************/
  /**
   * Executes the sort operation by applying sorted axis mapping.
   */
  @Override
  public void redo()
  {
    // apply the post-sorted index mapping
    m_axis.setMapping( m_newOrder );
    m_view.redraw();
  }

  /********************************************** undo ********************************************/
  /**
   * Reverses the sort operation by restoring original axis mapping.
   */
  @Override
  public void undo()
  {
    // apply the before-sort index mapping
    m_axis.setMapping( m_oldOrder );
    m_view.redraw();
  }

  /********************************************* text *********************************************/
  /**
   * Returns a description of this command for undo/redo UI.
   * <p>
   * Lazily constructs description including view ID if available.
   * 
   * @return command description
   */
  @Override
  public String text()
  {
    // construct command description
    if ( m_text == null )
    {
      m_text = "Sorted " + ( m_axis == m_view.getRowsAxis() ? "column " : "row " ) + m_label;

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }
}
