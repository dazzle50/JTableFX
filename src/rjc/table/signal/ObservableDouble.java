/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

/*************************************************************************************************/
/***************************** Observable double & read-only double ******************************/
/*************************************************************************************************/

public class ObservableDouble implements ISignal
{
  private double         m_value;    // stored double value
  private ReadOnlyDouble m_readonly; // read-only version of this observable

  public class ReadOnlyDouble implements ISignal // provides read-only access
  {
    private ObservableDouble m_observable;

    public ReadOnlyDouble( ObservableDouble observable )
    {
      // construct and propagate any signals
      m_observable = observable;
      m_observable.addListener( ( sender, oldValue ) -> signal( oldValue ) );
    }

    public double get()
    {
      // return value
      return m_observable.get();
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "="
          + this.get();
    }
  }

  /**************************************** constructor ******************************************/
  public ObservableDouble()
  {
    // construct
  }

  /**************************************** constructor ******************************************/
  public ObservableDouble( double value )
  {
    // construct
    m_value = value;
  }

  /********************************************* get *********************************************/
  public double get()
  {
    // return value of double
    return m_value;
  }

  /********************************************* set *********************************************/
  public void set( double newValue )
  {
    // set value of double, and signal if change
    if ( newValue != m_value )
    {
      double oldValue = m_value;
      m_value = newValue;
      signal( oldValue );
    }
  }

  /***************************************** getReadOnly *****************************************/
  public ReadOnlyDouble getReadOnly()
  {
    // return read-only version of double
    if ( m_readonly == null )
      m_readonly = new ReadOnlyDouble( this );
    return m_readonly;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return class string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "=" + this.get();
  }

}