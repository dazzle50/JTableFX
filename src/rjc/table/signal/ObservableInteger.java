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

public class ObservableInteger implements ISignal
{
  private int             m_value;    // stored integer value
  private ReadOnlyInteger m_readonly; // read-only version of this observable

  public class ReadOnlyInteger implements ISignal // provides read-only access
  {
    private ObservableInteger m_observable;

    public ReadOnlyInteger( ObservableInteger observable )
    {
      // construct and propagate any signals
      m_observable = observable;
      m_observable.addListener( ( sender, oldValue ) -> signal( oldValue ) );
    }

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
  public ObservableInteger()
  {
    // construct
  }

  /**************************************** constructor ******************************************/
  public ObservableInteger( int value )
  {
    // construct
    m_value = value;
  }

  /********************************************* get *********************************************/
  public int get()
  {
    // return value of integer
    return m_value;
  }

  /********************************************* set *********************************************/
  public void set( int newValue )
  {
    // set value of integer, and signal (this, old value) if change
    if ( newValue != m_value )
    {
      int oldValue = m_value;
      m_value = newValue;
      signal( oldValue );
    }
  }

  /***************************************** getReadOnly *****************************************/
  public ReadOnlyInteger getReadOnly()
  {
    // return read-only version of integer
    if ( m_readonly == null )
      m_readonly = new ReadOnlyInteger( this );
    return m_readonly;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return class string
    return Utils.name( this ) + "=" + this.get();
  }

}