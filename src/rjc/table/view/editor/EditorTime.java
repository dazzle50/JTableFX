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

package rjc.table.view.editor;

import rjc.table.control.TimeField;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/********************************** Table cell editor for times **********************************/
/*************************************************************************************************/

public class EditorTime extends AbstractCellEditor
{
  private TimeField m_editor = new TimeField();

  /**************************************** constructor ******************************************/
  public EditorTime()
  {
    // create table cell editor for time
    super();
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor time value
    return m_editor.getTime();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setTime( Time.now() );
    else if ( value instanceof Time )
      m_editor.setTime( (Time) value );
    else if ( value instanceof String )
    {
      // seed editor with a valid time before setting with input string which may not be a valid time
      m_editor.setTime( Time.now() );
      m_editor.setText( (String) value );
    }
    else
      throw new IllegalArgumentException( "Don't know how to handle " + value.getClass() + " " + value );
  }

}