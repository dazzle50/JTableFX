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

package rjc.table.view.action;

/*************************************************************************************************/
/****************** Weak-keyed reference counter to track filtered columns/rows ******************/
/*************************************************************************************************/

/**
 * Specialisation of {@link WeakSortMap} that maintains a positive integer reference count
 * per ({@code K} owner, integer index) pair.
 * <p>
 * When a count reaches zero it is removed automatically so {@link #hasFilter} reliably
 * indicates whether a count is active. {@link #increment} and {@link #decrement} each
 * perform a single inner-map operation via {@code merge} and {@code compute} respectively,
 * avoiding the double-lookup cost of a naive get-then-put approach.
 * <pre>
 *   // Filter.java
 *   public static final WeakFilterCount&lt;TableView&gt; columnFilterCount = new WeakFilterCount&lt;&gt;();
 *   public static final WeakFilterCount&lt;TableView&gt; rowFilterCount    = new WeakFilterCount&lt;&gt;();
 *
 *   // call-site — no static wrapper methods needed
 *   Filter.columnFilterCount.increment( view, dataColumn );
 *   Filter.columnFilterCount.decrement( view, dataColumn );
 *   Filter.columnFilterCount.hasFilter( view, dataColumn );
 * </pre>
 *
 * @param <K> the owner key type (held via weak reference)
 */
public class WeakFilterCount<K> extends WeakSortMap<K, Integer>
{
  /***************************************** increment *******************************************/
  /**
   * Increments the count for the given owner and index by one.
   * Creates a mapping with value {@code 1} if none currently exists.
   * Performs a single inner-map operation via {@code merge}.
   *
   * @param owner the owner key
   * @param index the data index
   */
  public void increment( K owner, int index )
  {
    // computeIfAbsent + merge: single outer and single inner map operation
    m_map.computeIfAbsent( owner, k -> new java.util.HashMap<>( INNER_INITIAL_CAPACITY ) ).merge( index, 1,
        Integer::sum );
  }

  /***************************************** decrement *******************************************/
  /**
   * Decrements the count for the given owner and index by one.
   * Removes the mapping entirely when the count reaches zero.
   * Performs a single inner-map operation via {@code compute}.
   *
   * @param owner the owner key
   * @param index the data index
   */
  public void decrement( K owner, int index )
  {
    var inner = m_map.get( owner );
    if ( inner == null )
      return;

    // compute performs a single inner lookup; returning null removes the entry automatically
    inner.compute( index, ( k, v ) -> ( v != null && v > 1 ) ? v - 1 : null );

    // discard empty inner map immediately to prevent memory accumulation
    if ( inner.isEmpty() )
      m_map.remove( owner );
  }

  /******************************************* hasFilter *****************************************/
  /**
   * Returns {@code true} if a mapping exists for the given owner and index.
   *
   * @param owner the owner key
   * @param index the data index
   * @return {@code true} if a mapping is present, {@code false} otherwise
   */
  public boolean hasFilter( K owner, int index )
  {
    var inner = m_map.get( owner );
    return inner != null && inner.containsKey( index );
  }

}