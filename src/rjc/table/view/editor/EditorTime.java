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

import rjc.table.control.TimeField;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/********************************** Table cell editor for times **********************************/
/*************************************************************************************************/

/**
 * Table cell editor backed by a {@link TimeField}, returning a {@link Time} value.
 * <p>
 * Values may be times, strings, or null. Null selects the current time; strings are parsed after
 * seeding the field with the current time so incomplete or invalid text retains a valid value.
 * Typed time values use the field's display precision and can lose finer components. A non-null
 * table status is required when opening the editor.
 */
public class EditorTime extends AbstractCellEditor
{
  private TimeField m_editor;

  /**************************************** constructor ******************************************/
  /**
   * Creates a time editor showing all four drop-down components, initially set to the current hour.
   */
  public EditorTime()
  {
    // create table cell editor for time
    m_editor = new TimeField();
    setControl( m_editor );
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates a time editor with selected drop-down components, initially set to the current hour.
   * Display precision includes all components through the most precise visible field, including
   * intermediate components whose individual fields are hidden.
   *
   * @param showHours whether to show the hours field
   * @param showMins  whether to show the minutes field
   * @param showSecs  whether to show the seconds field
   * @param showMilli whether to show the milliseconds field
   * @throws IllegalArgumentException if all four fields are hidden
   */
  public EditorTime( boolean showHours, boolean showMins, boolean showSecs, boolean showMilli )
  {
    // create time-editor with specified time components in drop-down
    m_editor = new TimeField( showHours, showMins, showSecs, showMilli );
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the most recent valid time, including while the field text is invalid.
   *
   * @return current {@link Time}
   */
  @Override
  public Object getValue()
  {
    // get editor time value
    return m_editor.getTime();
  }

  /******************************************* setValue ******************************************/
  /**
   * Displays a time, the current time for null, or supplied text after seeding the field with the
   * current time. Typed times use {@link TimeField#setTime(Time)} and its precision and signal
   * behaviour. Invalid string text leaves the seeded value available through {@link #getValue()}.
   *
   * @param value {@link Time}, string to parse, or null for the current time
   * @throws IllegalArgumentException if {@code value} is neither a time, a string, nor null
   */
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setTime( Time.now() );
    else if ( value instanceof Time time )
      m_editor.setTime( time );
    else if ( value instanceof String str )
    {
      // seed editor with a valid time before setting with input string which may not be a valid time
      m_editor.setTime( Time.now() );
      m_editor.setText( str );
    }
    else
      throw new IllegalArgumentException( "Don't know how to handle " + value.getClass() + " " + value );
  }

}