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

import rjc.table.control.DateField;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/******************************* Table cell editor for Date fields *******************************/
/*************************************************************************************************/

/**
 * Table cell editor backed by a {@link DateField}, returning a {@link Date} value.
 * <p>
 * Values may be dates, strings, or null. Null selects today; strings are parsed after seeding the
 * field with today so incomplete or invalid text retains a valid date. A non-null table status is
 * required when opening the editor.
 */
public class EditorDate extends AbstractCellEditor
{
  DateField m_editor = new DateField(); // date editor

  /**************************************** constructor ******************************************/
  /**
   * Creates a date editor initially set to today.
   */
  public EditorDate()
  {
    // create date table cell editor
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the most recent valid date, including while the field text is invalid.
   *
   * @return current {@link Date}
   */
  @Override
  public Object getValue()
  {
    // get editor date value
    return m_editor.getDate();
  }

  /******************************************* setValue ******************************************/
  /**
   * Displays a date, today's date for null, or supplied text after seeding the field with today.
   * Invalid string text leaves the seeded date available through {@link #getValue()}.
   *
   * @param value {@link Date}, string to parse, or null for today
   * @throws IllegalArgumentException if {@code value} is neither a date, a string, nor null
   */
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setDate( Date.now() );
    else if ( value instanceof Date date )
      m_editor.setDate( date );
    else if ( value instanceof String str )
    {
      // seed editor with a valid date before setting with input string which may not be a valid date
      m_editor.setDate( Date.now() );
      m_editor.setText( str );
    }
    else
      throw new IllegalArgumentException( "Don't know how to handle " + value.getClass() + " " + value );
  }

}
