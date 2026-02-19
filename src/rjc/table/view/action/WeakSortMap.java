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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import rjc.table.data.TableData;

/*************************************************************************************************/
/******************** Weak-keyed reference value to track sorted columns/rows ********************/
/*************************************************************************************************/

/**
 * Maps an ({@code K} owner, integer index) pair to a value of type {@code V}.
 * <p>
 * Owner keys are held via weak references so entries are discarded automatically
 * when an owner is garbage-collected. Inner maps are removed the moment they become
 * empty, preventing unbounded growth over the application lifetime.
 * <p>
 * The name reflects the two implementation details callers must understand:
 * the outer key is <em>weak</em>, and the inner key is an integer <em>index</em>.
 * The type parameter {@code K} deliberately avoids coupling to any specific owner
 * type, allowing use with {@link TableView}, {@link TableData}, or any future owner:
 * <pre>
 *   // Sort.java
 *   public static final WeakSortMap&lt;TableView, SortType&gt; columnSort = new WeakSortMap&lt;&gt;();
 *   public static final WeakSortMap&lt;TableView, SortType&gt; rowSort    = new WeakSortMap&lt;&gt;();
 *
 *   // call-site — no static wrapper methods needed
 *   Sort.columnSort.put( view, dataColumn, SortType.ASCENDING );
 *   Sort.columnSort.get( view, dataColumn, SortType.NOTSORTED );
 * </pre>
 *
 * @param <K> the owner key type (held via weak reference)
 * @param <V> the type of mapped values
 */
public class WeakSortMap<K, V>
{
  // initial capacity for inner maps — most tables have few filtered/sorted columns or rows
  protected static final int              INNER_INITIAL_CAPACITY = 4;

  protected final Map<K, Map<Integer, V>> m_map                  = new WeakHashMap<>();

  /********************************************** get ********************************************/
  /**
   * Returns the value associated with the given owner and index,
   * or {@code defaultValue} if no mapping exists.
   *
   * @param owner        the owner key
   * @param index        the data index
   * @param defaultValue returned when no mapping is present
   * @return the mapped value, or {@code defaultValue}
   */
  public V get( K owner, int index, V defaultValue )
  {
    // single outer lookup — avoids creating an unnecessary inner map
    var inner = m_map.get( owner );
    return inner != null ? inner.getOrDefault( index, defaultValue ) : defaultValue;
  }

  /********************************************** put ********************************************/
  /**
   * Associates {@code value} with the given owner and index,
   * creating the inner map for the owner if one does not yet exist.
   *
   * @param owner the owner key
   * @param index the data index
   * @param value the value to store; must not be {@code null}
   */
  public void put( K owner, int index, V value )
  {
    // computeIfAbsent performs a single outer lookup regardless of whether the inner map exists
    m_map.computeIfAbsent( owner, k -> new HashMap<>( INNER_INITIAL_CAPACITY ) ).put( index, value );
  }

  /******************************************** remove *******************************************/
  /**
   * Removes the mapping for the given owner and index.
   * The owner entry is also removed if its inner map becomes empty after removal.
   *
   * @param owner the owner key
   * @param index the data index
   */
  public void remove( K owner, int index )
  {
    var inner = m_map.get( owner );
    if ( inner != null )
    {
      inner.remove( index );
      // discard empty inner map immediately to prevent memory accumulation
      if ( inner.isEmpty() )
        m_map.remove( owner );
    }
  }

}