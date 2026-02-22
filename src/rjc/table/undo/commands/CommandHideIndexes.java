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
import rjc.table.view.action.Filter;
import rjc.table.view.action.WeakFilterCount;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************** UndoCommand for hiding columns or rows on a table-view *********************/
/*************************************************************************************************/

/**
 * Undoable command for hiding columns or rows in a table view.
 * <p>
 * When created by a filter operation, {@code filterDataIndex} identifies the column or row
 * that the filter is applied to. The corresponding {@link Filter#columnFilterCount} or
 * {@link Filter#rowFilterCount} entry is incremented on construction and {@link #redo()},
 * and decremented on {@link #undo()}, so the overlay can display funnel indicators.
 * Manual hide operations pass {@link #NO_FILTER} to suppress filter-count management entirely.
 *
 * @see IUndoCommand
 * @see TableView
 * @see TableAxis
 */
public class CommandHideIndexes implements IUndoCommand
{
  private final TableView                  m_view;            // table view
  private final TableAxis                  m_axis;            // axis for hiding
  private final WeakFilterCount<TableView> m_filterCount;     // resolved filter map, or null for manual hide
  private final int                        m_filterDataIndex; // filter key data index; unused when m_filterCount null
  private HashSetInt                       m_indexes;         // data-indexes actually hidden
  private String                           m_text;            // lazily constructed description for undo/redo UI

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a manual hide command with no filter-count tracking.
   *
   * @param view        the table view
   * @param axis        the axis on which to hide
   * @param viewIndexes the set of view indices to hide
   */
  public CommandHideIndexes( TableView view, TableAxis axis, HashSetInt viewIndexes )
  {
    this( view, axis, viewIndexes, TableAxis.INVALID );
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a hide command, optionally tracking filter state for overlay highlighting.
   * <p>
   * Resolves the appropriate filter-count map from {@code axis} at construction time and stores
   * it directly, avoiding any per-call axis check in {@link #redo()} and {@link #undo()}.
   * Increments the map entry — but only when at least one index was actually hidden.
   *
   * @param view            the table view
   * @param axis            the axis on which to hide
   * @param viewIndexes     the set of view indices to hide
   * @param filterDataIndex data index of the column or row used as the filter key,
   *                        or {@link TableAxis#INVALID} for a manual hide
   */
  public CommandHideIndexes( TableView view, TableAxis axis, HashSetInt viewIndexes, int filterDataIndex )
  {
    // prepare hide command
    m_view = view;
    m_axis = axis;
    m_filterDataIndex = filterDataIndex;

    // resolve filter map once; null signals no filter tracking required
    m_filterCount = filterDataIndex != TableAxis.INVALID
        ? ( axis == view.getRowsAxis() ? Filter.columnFilterCount : Filter.rowFilterCount )
        : null;

    // convert view indexes to data indexes and hide them
    HashSetInt dataIndexes = new HashSetInt( viewIndexes.size() );
    var it = viewIndexes.iterator();
    while ( it.hasNext() )
      dataIndexes.add( m_axis.getDataIndex( it.next() ) );

    m_indexes = m_axis.hide( dataIndexes );

    // increment filter count only when filter-driven and something was actually hidden
    if ( m_filterCount != null && m_indexes != null && !m_indexes.isEmpty() )
      m_filterCount.increment( m_view, m_filterDataIndex );

    m_view.redraw();
  }

  /******************************************* redo **********************************************/
  /**
   * Executes the hide operation and increments filter count if filter-driven.
   */
  @Override
  public void redo()
  {
    // hide the indexes and increment filter count if filter-driven
    m_axis.hide( m_indexes );
    if ( m_filterCount != null )
      m_filterCount.increment( m_view, m_filterDataIndex );
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  /**
   * Reverses the hide operation and decrements filter count if filter-driven.
   */
  @Override
  public void undo()
  {
    // unhide the indexes and decrement filter count if filter-driven
    m_axis.unhide( m_indexes );
    if ( m_filterCount != null )
      m_filterCount.decrement( m_view, m_filterDataIndex );
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  /**
   * Returns a description of this command for undo/redo UI.
   *
   * @return command description
   */
  @Override
  public String text()
  {
    // construct command description lazily
    if ( m_text == null )
    {
      String verb = m_filterCount != null ? "Filter " : "Hide ";
      m_text = verb + m_indexes.size() + ( m_axis == m_view.getColumnsAxis() ? " column" : " row" )
          + ( m_indexes.size() > 1 ? "s" : "" );

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Checks if this command is valid.
   *
   * @return {@code true} if at least one index was hidden
   */
  @Override
  public boolean isValid()
  {
    // command is valid only if some indexes were hidden (indicated by m_indexes not null)
    return m_indexes != null;
  }

}