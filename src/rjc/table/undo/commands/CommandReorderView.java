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

import rjc.table.HashSetInt;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cursor.AbstractReorderCursor;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows on table-view ********************/
/*************************************************************************************************/

/**
 * Undoable command for reordering view presentation without modifying underlying data.
 * <p>
 * Moves selected view positions to a target position by manipulating the axis
 * mapping. Unlike {@link CommandReorderData}, this only affects how the table
 * is displayed, leaving actual data storage order unchanged.
 * <p>
 * Example: Moving view positions [0,1] to position 3 in [0,1,2,3,4] produces [2,0,1,3,4].
 * 
 * @see IUndoCommand
 * @see TableView
 * @see TableAxis
 * @see CommandReorderData
 */
public class CommandReorderView implements IUndoCommand
{
  private TableView  m_view;        // table view
  private TableAxis  m_axis;        // axis being reordered
  private HashSetInt m_viewIndexes; // view-indices being moved
  private int        m_insertIndex; // insert position (view index)
  private String     m_text;        // text describing command

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a view reordering command.
   * <p>
   * Tests if reorder actually changes the mapping via hash comparison. If no
   * change occurs, command is marked invalid to prevent cluttering undo stack.
   * 
   * @param view            the table view to reorder
   * @param axis            the axis (rows or columns) being reordered
   * @param viewIndexes     set of view-indices to move
   * @param targetViewIndex view-index where selected items should be inserted
   */
  public CommandReorderView( TableView view, TableAxis axis, HashSetInt viewIndexes, int targetViewIndex )
  {
    // prepare reorder command
    m_view = view;
    m_axis = axis;
    m_viewIndexes = viewIndexes;
    m_insertIndex = targetViewIndex;

    // test if reorder changes mapping, if not make command invalid
    var hash = m_axis.getIndexMappingHash();
    redo();
    if ( hash == m_axis.getIndexMappingHash() )
      m_view = null;
  }

  /******************************************* redo **********************************************/
  /**
   * Executes the reordering operation by updating axis mapping.
   */
  @Override
  public void redo()
  {
    // action command and redraw view
    m_axis.reorderView( m_viewIndexes, m_insertIndex );
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  /**
   * Reverses the reordering operation.
   * <p>
   * Moves items back one-by-one in predictable order, adjusting positions
   * dynamically as each item returns to its original location.
   */
  @Override
  public void undo()
  {
    // revert command
    int countBeforeMoves = AbstractReorderCursor.countBefore( m_viewIndexes, m_insertIndex );
    int countAfterMoves = m_viewIndexes.size() - countBeforeMoves;

    // create ordered list to process moves in predictable order
    var sortedMovedViewIndices = m_viewIndexes.toSortedArray();

    // move columns or rows back to their prior positions
    HashSetInt singleIndexSet = new HashSetInt();
    for ( int originalViewPos : sortedMovedViewIndices )
      if ( m_insertIndex > originalViewPos )
      {
        singleIndexSet.clear();
        singleIndexSet.add( m_insertIndex - countBeforeMoves );
        m_axis.reorderView( singleIndexSet, originalViewPos );
        countBeforeMoves--;
      }
      else
      {
        singleIndexSet.clear();
        singleIndexSet.add( m_insertIndex );
        m_axis.reorderView( singleIndexSet, originalViewPos + countAfterMoves );
        countAfterMoves--;
      }

    // redraw table in this view only
    m_view.redraw();
  }

  /******************************************* text **********************************************/
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
    // command description
    if ( m_text == null )
    {
      m_text = "Moved " + m_viewIndexes.size() + ( m_axis == m_view.getColumnsAxis() ? " column" : " row" )
          + ( m_viewIndexes.size() > 1 ? "s" : "" );

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Checks if this command is valid.
   * <p>
   * Command is invalid if the reorder operation resulted in no actual change
   * to the axis mapping, as detected by hash comparison during construction.
   *
   * @return {@code true} if items were moved
   */
  @Override
  public boolean isValid()
  {
    // command is valid only if reorder results in difference (via hashcode)
    return m_view != null;
  }

}