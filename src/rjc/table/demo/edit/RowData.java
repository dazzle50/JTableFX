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

package rjc.table.demo.edit;

import rjc.table.Utils;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/**************************** Contains one row of editable-table data ****************************/
/*************************************************************************************************/

public class RowData
{
  public enum Column
  {
    ReadOnly, Text, Integer, Double, Date, Time, DateTime, Select, MAX
  }

  // private variables containing the row's data
  private String   m_readonly;
  private String   m_text;
  private int      m_integer;
  private double   m_double;
  private Date     m_date;
  private Time     m_time;
  private DateTime m_datetime;
  private Fruit    m_fruit;

  public enum Fruit
  {
    Apple, Banana, Pear, Plum, Orange, Cherry
  }

  /**************************************** constructor ******************************************/
  public RowData( int id )
  {
    // populate the private variables with some contents
    m_readonly = "Read-only text " + ( id + 1 );
    m_text = "Editable text " + ( id + 1 );
    m_integer = id + 100;
    m_double = id + 10.0;
    m_date = Date.now().plusDays( id * 5 - 20 );
    m_time = Time.now();
    m_time.addMilliseconds( id * 12345678 );
    m_datetime = new DateTime( m_date, m_time );
    m_fruit = Fruit.values()[id % Fruit.values().length];
  }

  /****************************************** getValue *******************************************/
  public Object getValue( int dataColumn )
  {
    // return row value for specified column index
    switch ( Column.values()[dataColumn] )
    {
      case ReadOnly:
        return m_readonly;
      case Text:
        return m_text;
      case Integer:
        return m_integer;
      case Double:
        return m_double;
      case Date:
        return m_date;
      case Time:
        return m_time;
      case DateTime:
        return m_datetime;
      case Select:
        return m_fruit;
      default:
        throw new IllegalArgumentException( "Section = " + dataColumn );
    }
  }

  /***************************************** processValue ****************************************/
  public String processValue( int dataColumn, Object newValue, Boolean setValue )
  {
    // set/check field value and return null if successful/possible
    switch ( Column.values()[dataColumn] )
    {
      case ReadOnly:
        // can never set ReadOnly field
        return "Read-only";

      case Text:
        // can always set Text field
        if ( setValue )
          m_text = newValue == null ? null : newValue.toString();
        return null;

      case Integer:
        // check new value is integer and in range
        if ( newValue instanceof Integer newInt )
        {
          if ( newInt < 0 || newInt > 999 )
            return "Value not between 0 and 999";
          if ( setValue )
            m_integer = newInt;
          return null;
        }
        return "Not integer: " + Utils.objectsString( newValue );

      case Double:
        // check new value is double and in range
        if ( newValue instanceof Double newDouble )
        {
          if ( newDouble < 0.0 || newDouble > 999.0 )
            return "Value not between 0.0 and 999.0";
          if ( setValue )
            m_double = newDouble;
          return null;
        }
        return "Not double: " + Utils.objectsString( newValue );

      case Date:
        // check new value is date
        if ( newValue instanceof Date newDate )
        {
          if ( setValue )
            m_date = newDate;
          return null;
        }
        return "Not date: " + Utils.objectsString( newValue );

      case Time:
        // check new value is time
        if ( newValue instanceof Time newTime )
        {
          if ( setValue )
            m_time = newTime;
          return null;
        }
        return "Not time: " + Utils.objectsString( newValue );

      case DateTime:
        // check new value is date-time
        if ( newValue instanceof DateTime newDT )
        {
          if ( setValue )
            m_datetime = newDT;
          return null;
        }
        return "Not date-time: " + Utils.objectsString( newValue );

      case Select:
        // comment TODO
        return "TODO";

      default:
        return "Not implemented";
    }

  }

}
