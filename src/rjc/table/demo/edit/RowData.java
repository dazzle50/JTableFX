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

}
