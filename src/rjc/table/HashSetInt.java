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

package rjc.table;

import java.util.Arrays;
import java.util.NoSuchElementException;

/*************************************************************************************************/
/******************* HashSet for primitive int values avoiding boxing overhead *******************/
/*************************************************************************************************/

/**
 * A hash set implementation for primitive int values that avoids boxing overhead.
 * This class provides constant-time performance for basic operations (add, remove, contains)
 * assuming the hash function disperses elements properly among the buckets.
 */
public class HashSetInt
{
  private static final int   DEFAULT_CAPACITY    = 16;
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;
  private static final int   EMPTY               = Integer.MIN_VALUE;
  private static final int   DELETED             = Integer.MIN_VALUE + 1;

  private int[]              m_table;
  private int                m_size;
  private int                m_deletedCount;
  private final float        m_loadFactor;
  private int                m_threshold;

  /**************************************** constructor ******************************************/
  /**
   * Constructs an empty HashSetInt with default initial capacity (16) and load factor (0.75).
   */
  public HashSetInt()
  {
    this( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR );
  }

  /**************************************** constructor ******************************************/
  /**
   * Constructs an empty HashSetInt with the specified initial capacity and default load factor (0.75).
   * 
   * @param initialCapacity the initial capacity
   * @throws IllegalArgumentException if the initial capacity is negative
   */
  public HashSetInt( int initialCapacity )
  {
    this( initialCapacity, DEFAULT_LOAD_FACTOR );
  }

  /**************************************** constructor ******************************************/
  /**
   * Constructs an empty HashSetInt with the specified initial capacity and load factor.
   * 
   * @param initialCapacity the initial capacity
   * @param loadFactor the load factor
   * @throws IllegalArgumentException if the initial capacity is negative or load factor is non-positive
   */
  public HashSetInt( int initialCapacity, float loadFactor )
  {
    if ( initialCapacity < 0 )
      throw new IllegalArgumentException( "Illegal capacity: " + initialCapacity );
    if ( loadFactor <= 0 || Float.isNaN( loadFactor ) )
      throw new IllegalArgumentException( "Illegal load factor: " + loadFactor );

    m_loadFactor = loadFactor;
    m_table = new int[tableSizeFor( initialCapacity )];
    Arrays.fill( m_table, EMPTY );
    m_threshold = (int) ( m_table.length * m_loadFactor );
  }

  /*************************************** tableSizeFor ******************************************/
  private int tableSizeFor( int capacity )
  {
    // find next power of two >= capacity
    int n = capacity - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return ( n < 0 ) ? 1 : n + 1;
  }

  /********************************************* add *********************************************/
  /**
   * Adds the specified value to this set if it is not already present.
   * 
   * @param value the value to be added
   * @return true if this set did not already contain the specified value
   * @throws IllegalArgumentException if value is Integer.MIN_VALUE or Integer.MIN_VALUE + 1 (reserved values)
   */
  public boolean add( int value )
  {
    if ( value == EMPTY || value == DELETED )
      throw new IllegalArgumentException( "Cannot add reserved value: " + value );

    // check if resize needed
    if ( m_size + m_deletedCount >= m_threshold )
      resize();

    int index = findSlot( value );

    // value already exists
    if ( m_table[index] == value )
      return false;

    // found empty or deleted slot
    if ( m_table[index] == DELETED )
      m_deletedCount--;

    m_table[index] = value;
    m_size++;
    return true;
  }

  /****************************************** contains *******************************************/
  /**
   * Returns true if this set contains the specified value.
   * 
   * @param value the value whose presence is to be tested
   * @return true if this set contains the specified value
   */
  public boolean contains( int value )
  {
    if ( value == EMPTY || value == DELETED )
      return false;

    int index = findSlot( value );
    return m_table[index] == value;
  }

  /******************************************* remove ********************************************/
  /**
   * Removes the specified value from this set if it is present.
   * 
   * @param value the value to be removed
   * @return true if the set contained the specified value
   */
  public boolean remove( int value )
  {
    if ( value == EMPTY || value == DELETED )
      return false;

    int index = findSlot( value );

    if ( m_table[index] != value )
      return false;

    // mark as deleted instead of empty to maintain probe sequences
    m_table[index] = DELETED;
    m_size--;
    m_deletedCount++;

    // resize if too many deleted entries
    if ( m_deletedCount > m_size )
      resize();

    return true;
  }

  /***************************************** findSlot ********************************************/
  private int findSlot( int value )
  {
    int hash = hash( value );
    int index = hash & ( m_table.length - 1 );
    int firstDeleted = -1;

    // linear probing
    while ( m_table[index] != EMPTY )
    {
      if ( m_table[index] == value )
        return index;

      // remember first deleted slot for potential insertion
      if ( m_table[index] == DELETED && firstDeleted == -1 )
        firstDeleted = index;

      index = ( index + 1 ) & ( m_table.length - 1 );
    }

    // return deleted slot if found, otherwise empty slot
    return firstDeleted != -1 ? firstDeleted : index;
  }

  /******************************************** hash *********************************************/
  private int hash( int value )
  {
    // spread bits using supplemental hash function
    value ^= ( value >>> 16 );
    value *= 0x85ebca6b;
    value ^= ( value >>> 13 );
    value *= 0xc2b2ae35;
    value ^= ( value >>> 16 );
    return value;
  }

  /******************************************* resize ********************************************/
  private void resize()
  {
    int[] oldTable = m_table;
    m_table = new int[oldTable.length * 2];
    Arrays.fill( m_table, EMPTY );
    m_threshold = (int) ( m_table.length * m_loadFactor );
    m_size = 0;
    m_deletedCount = 0;

    // rehash all non-empty, non-deleted entries
    for ( int value : oldTable )
      if ( value != EMPTY && value != DELETED )
        add( value );
  }

  /******************************************** size *********************************************/
  /**
   * Returns the number of elements in this set.
   * 
   * @return the number of elements in this set
   */
  public int size()
  {
    return m_size;
  }

  /****************************************** isEmpty ********************************************/
  /**
   * Returns true if this set contains no elements.
   * 
   * @return true if this set contains no elements
   */
  public boolean isEmpty()
  {
    return m_size == 0;
  }

  /******************************************* clear *********************************************/
  /**
   * Removes all elements from this set.
   */
  public void clear()
  {
    Arrays.fill( m_table, EMPTY );
    m_size = 0;
    m_deletedCount = 0;
  }

  /****************************************** toArray ********************************************/
  /**
   * Returns an array containing all of the elements in this set.
   * 
   * @return an array containing all elements in this set
   */
  public int[] toArray()
  {
    int[] result = new int[m_size];
    int pos = 0;

    // collect all non-empty, non-deleted values
    for ( int value : m_table )
      if ( value != EMPTY && value != DELETED )
        result[pos++] = value;

    return result;
  }

  /****************************************** iterator *******************************************/
  /**
   * Returns an iterator over the elements in this set.
   * 
   * @return an iterator over the elements in this set
   */
  public IntIterator iterator()
  {
    return new IntIterator();
  }

  /**
   * Iterator for IntHashSet that avoids boxing.
   */
  public class IntIterator
  {
    private int m_currentIndex = 0;
    private int m_remaining    = m_size;

    /**
     * Returns true if the iteration has more elements.
     * 
     * @return true if the iteration has more elements
     */
    public boolean hasNext()
    {
      return m_remaining > 0;
    }

    /**
     * Returns the next element in the iteration.
     * 
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public int next()
    {
      if ( !hasNext() )
        throw new NoSuchElementException();

      // find next valid entry
      while ( m_currentIndex < m_table.length )
      {
        int value = m_table[m_currentIndex++];
        if ( value != EMPTY && value != DELETED )
        {
          m_remaining--;
          return value;
        }
      }

      throw new NoSuchElementException();
    }
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    if ( isEmpty() )
      return "[]";

    StringBuilder sb = new StringBuilder( "[" );
    boolean first = true;

    for ( int value : m_table )
    {
      if ( value != EMPTY && value != DELETED )
      {
        if ( !first )
          sb.append( ", " );
        sb.append( value );
        first = false;
      }
    }

    sb.append( "]" );
    return sb.toString();
  }
}