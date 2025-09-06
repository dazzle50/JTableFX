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
/***************************** Observable double & read-only double ******************************/
/*************************************************************************************************/

/**
 * Observable double value that can signal listeners when changed.
 * Provides both mutable and read-only access patterns with proper double comparison
 * for change detection to handle floating-point precision issues.
 */
public class ObservableDouble implements ISignal
{
  private double         m_value;    // stored double value
  private ReadOnlyDouble m_readonly; // read-only version of this observable

  /**
   * Read-only wrapper for ObservableDouble that provides immutable access
   * to the underlying value while still receiving change notifications.
   */
  public static class ReadOnlyDouble implements ISignal // provides read-only access
  {
    private final ObservableDouble m_observable;

    /**
     * Creates a read-only view of the specified observable double.
     * Automatically forwards change signals from the underlying observable.
     * 
     * @param observable the observable double to wrap
     */
    public ReadOnlyDouble( ObservableDouble observable )
    {
      // construct and propagate any signals
      m_observable = observable;
      m_observable.addListener( ( sender, oldValue ) -> signal( oldValue ) );
    }

    /**
     * Gets the current value of the observable double.
     * 
     * @return the current double value
     */
    public double get()
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
   * Creates an observable double with default value of 0.0.
   */
  public ObservableDouble()
  {
    // construct with default zero value
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable double with the specified initial value.
   * 
   * @param value the initial double value
   */
  public ObservableDouble( double value )
  {
    // construct with specified initial value
    m_value = value;
  }

  /********************************************* get *********************************************/
  /**
   * Gets the current value of this observable double.
   * 
   * @return the current double value
   */
  public double get()
  {
    // return current value
    return m_value;
  }

  /********************************************* set *********************************************/
  /**
   * Sets a new value for this observable double.
   * If the new value differs from the current value (using proper double comparison),
   * signals all listeners with the previous value.
   * 
   * @param newValue the new double value to set
   */
  public void set( double newValue )
  {
    // set value and signal if changed using proper double comparison
    if ( Double.compare( newValue, m_value ) != 0 )
    {
      double oldValue = m_value;
      m_value = newValue;
      signal( oldValue );
    }
  }

  /***************************************** getReadOnly *****************************************/
  /**
   * Gets a read-only view of this observable double.
   * The same read-only instance is returned on subsequent calls (lazy initialisation).
   * 
   * @return a read-only wrapper that provides immutable access to this observable
   */
  public ReadOnlyDouble getReadOnly()
  {
    // return lazily-created read-only wrapper
    if ( m_readonly == null )
      m_readonly = new ReadOnlyDouble( this );
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