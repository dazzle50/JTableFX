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

package rjc.table.signal;

import rjc.table.Utils;

/*************************************************************************************************/
/**************************** Observable integer & read-only integer *****************************/
/*************************************************************************************************/

/**
 * Observable integer value that can signal listeners when changed.
 * Provides both mutable and read-only access patterns with proper integer comparison
 * for change detection.
 */
public class ObservableInteger implements ISignal
{
  private int             m_value;    // stored integer value
  private ReadOnlyInteger m_readonly; // read-only version of this observable

  /**
   * Read-only wrapper for ObservableInteger that provides immutable access
   * to the underlying value while still receiving change notifications.
   */
  public static class ReadOnlyInteger implements ISignal // provides read-only access
  {
    private final ObservableInteger m_observable;

    /**
     * Creates a read-only view of the specified observable integer.
     * Automatically forwards change signals from the underlying observable.
     * 
     * @param observable the observable integer to wrap
     */
    public ReadOnlyInteger( ObservableInteger observable )
    {
      // construct and propagate any signals
      m_observable = observable;
      m_observable.addListener( ( sender, oldValue ) -> signal( oldValue ) );
    }

    /**
     * Gets the current value of the observable integer.
     * 
     * @return the current integer value
     */
    public int get()
    {
      // return value
      return m_observable.get();
    }

    @Override
    public String toString()
    {
      return Utils.name( this ) + "=" + this.get();
    }
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable integer with default value of 0.
   */
  public ObservableInteger()
  {
    // construct with default zero value
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable integer with the specified initial value.
   * 
   * @param value the initial integer value
   */
  public ObservableInteger( int value )
  {
    // construct with specified initial value
    m_value = value;
  }

  /********************************************* get *********************************************/
  /**
   * Gets the current value of this observable integer.
   * 
   * @return the current integer value
   */
  public int get()
  {
    // return current value
    return m_value;
  }

  /********************************************* set *********************************************/
  /**
   * Sets a new value for this observable integer.
   * If the new value differs from the current value,
   * signals all listeners with the previous value.
   * 
   * @param newValue the new integer value to set
   */
  public void set( int newValue )
  {
    // set value and signal if changed
    if ( newValue != m_value )
    {
      int oldValue = m_value;
      m_value = newValue;
      signal( oldValue );
    }
  }

  /***************************************** getReadOnly *****************************************/
  /**
   * Gets a read-only view of this observable integer.
   * The same read-only instance is returned on subsequent calls (lazy initialisation).
   * 
   * @return a read-only wrapper that provides immutable access to this observable
   */
  public ReadOnlyInteger getReadOnly()
  {
    // return lazily-created read-only wrapper
    if ( m_readonly == null )
      m_readonly = new ReadOnlyInteger( this );
    return m_readonly;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return class name and current value
    return Utils.name( this ) + "=" + this.get();
  }

}