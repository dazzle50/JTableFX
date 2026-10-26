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

package rjc.table.view.editor;

import rjc.table.control.DateTimeField;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/******************************* Table cell editor for date-times ********************************/
/*************************************************************************************************/

/**
 * Table cell editor backed by a {@link DateTimeField}, returning a {@link DateTime} value.
 * <p>
 * Values may be date-times, strings, or null. Null selects the current date-time; strings are
 * parsed after seeding the field with the current date-time so incomplete or invalid text retains
 * a valid value. A non-null table status is required when opening the editor.
 */
public class EditorDateTime extends AbstractCellEditor
{
  private DateTimeField m_editor = new DateTimeField();

  /**************************************** constructor ******************************************/
  /**
   * Creates a date-time editor initially set to the start of the current hour.
   */
  public EditorDateTime()
  {
    // create table cell editor for date-time
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the most recent valid date-time, including while the field text is invalid.
   *
   * @return current {@link DateTime}
   */
  @Override
  public Object getValue()
  {
    // get editor date-time value
    return m_editor.getDateTime();
  }

  /******************************************* setValue ******************************************/
  /**
   * Displays a date-time, the current date-time for null, or supplied text after seeding the field
   * with the current date-time. Invalid string text leaves the seeded value available through
   * {@link #getValue()}.
   *
   * @param value {@link DateTime}, string to parse, or null for the current date-time
   * @throws IllegalArgumentException if {@code value} is neither a date-time, a string, nor null
   */
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setDateTime( DateTime.now() );
    else if ( value instanceof DateTime dt )
      m_editor.setDateTime( dt );
    else if ( value instanceof String str )
    {
      // seed editor with a valid date-time before setting with input string which may not be a valid date-time
      m_editor.setDateTime( DateTime.now() );
      m_editor.setText( str );
    }
    else
      throw new IllegalArgumentException( "Don't know how to handle " + value.getClass() + " " + value );
  }

}