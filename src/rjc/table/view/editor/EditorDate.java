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

import rjc.table.control.DateField;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/******************************* Table cell editor for Date fields *******************************/
/*************************************************************************************************/

public class EditorDate extends AbstractCellEditor
{
  DateField m_editor = new DateField(); // date editor

  /**************************************** constructor ******************************************/
  public EditorDate()
  {
    // create date table cell editor
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor integer value
    return m_editor.getDate();
  }

  /******************************************* setValue ******************************************/
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
