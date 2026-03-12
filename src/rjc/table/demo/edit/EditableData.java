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

package rjc.table.demo.edit;

import rjc.table.Utils;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/**************************** Contains one row of editable-table data ****************************/
/*************************************************************************************************/

public class EditableData
{
  public enum Column
  {
    ReadOnly, Text, Integer, Double, Date, Time, DateTime, Select, MAX
  }

  private static final Column[] COLUMN_CACHE = Column.values();

  // private variables containing the row's data
  private String                m_readonly;
  private String                m_text;
  private int                   m_integer;
  private double                m_double;
  private Date                  m_date;
  private Time                  m_time;
  private DateTime              m_datetime;
  private Fruit                 m_fruit;

  public enum Fruit
  {
    Apple, Banana, Pear, Plum, Orange, Cherry
  }

  /**************************************** constructor ******************************************/
  public EditableData()
  {
    // empty row with default/blank contents
  }

  public EditableData( int id )
  {
    // populate the private variables with some contents
    m_readonly = "Read-only text " + ( id + 1 );
    m_text = "Editable text " + ( id + 1 );
    m_integer = 100 + (int) ( Math.random() * 100 );
    m_double = id + 10.0;
    m_date = Date.now().plusDays( id * 5 - 20 );
    m_time = Time.now().plusMilliseconds( id * 12345678 );
    m_datetime = DateTime.of( m_date, m_time );
    m_fruit = Fruit.values()[id % Fruit.values().length];
  }

  /****************************************** getValue *******************************************/
  public Object getValue( int dataColumn )
  {
    // return row value using cached column array and switch expression
    return switch ( COLUMN_CACHE[dataColumn] )
    {
      case ReadOnly -> m_readonly;
      case Text -> m_text;
      case Integer -> m_integer;
      case Double -> m_double;
      case Date -> m_date;
      case Time -> m_time;
      case DateTime -> m_datetime;
      case Select -> m_fruit;
      default -> throw new IllegalStateException( "Unexpected value: " + COLUMN_CACHE[dataColumn] );
    };
  }

  /****************************************** setValue *******************************************/
  public String setValue( int dataColumn, Object newValue, boolean commit )
  {
    // process value update or validation using a switch expression
    return switch ( COLUMN_CACHE[dataColumn] )
    {
      case ReadOnly -> "Read-only";

      case Text -> {
        if ( commit )
          m_text = newValue == null ? null : newValue.toString();
        yield null;
      }

      case Integer -> {
        // validate that the object is an integer and within the valid range
        if ( !( newValue instanceof Integer val ) )
          yield "Not integer: " + Utils.objectsString( newValue );
        if ( val < 1 || val > 999 )
          yield "Value not between 1 and 999";
        if ( commit )
          m_integer = val;
        yield null;
      }

      case Double -> {
        // validate that the object is a double and within the valid range
        if ( !( newValue instanceof Double val ) )
          yield "Not double: " + Utils.objectsString( newValue );
        if ( val < 0.0 || val > 999.0 )
          yield "Value not between 0.0 and 999.0";
        if ( commit )
          m_double = val;
        yield null;
      }

      case Date -> {
        // ensure the input is a date type or null before applying
        if ( newValue != null && !( newValue instanceof Date ) )
          yield "Not date: " + Utils.objectsString( newValue );
        if ( commit )
          m_date = (Date) newValue;
        yield null;
      }

      case Time -> {
        // ensure the input is a time type or null before applying
        if ( newValue != null && !( newValue instanceof Time ) )
          yield "Not time: " + Utils.objectsString( newValue );
        if ( commit )
          m_time = (Time) newValue;
        yield null;
      }

      case DateTime -> {
        // ensure the input is a date-time type or null before applying
        if ( newValue != null && !( newValue instanceof DateTime ) )
          yield "Not date-time: " + Utils.objectsString( newValue );
        if ( commit )
          m_datetime = (DateTime) newValue;
        yield null;
      }

      case Select -> {
        // ensure the input is a valid fruit selection or null
        if ( newValue != null && !( newValue instanceof Fruit ) )
          yield "Not fruit: " + Utils.objectsString( newValue );
        if ( commit )
          m_fruit = (Fruit) newValue;
        yield null;
      }

      default -> "Not implemented";
    };

  }

}
