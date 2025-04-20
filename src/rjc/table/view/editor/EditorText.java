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

import rjc.table.control.ExpandingField;

/*************************************************************************************************/
/******************************* Table cell editor for simple text *******************************/
/*************************************************************************************************/

public class EditorText extends AbstractCellEditor
{
  private ExpandingField editor = new ExpandingField();

  /**************************************** constructor ******************************************/
  public EditorText()
  {
    // create text table cell editor
    super();
    setControl( editor );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor text
    return editor.getText();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set editor text
    String str = value == null ? "" : value.toString();
    editor.setText( str );
  }

  /****************************************** setAllowed *****************************************/
  public void setAllowed( String regex )
  {
    // regular expression that limits what can be entered into editor
    editor.setAllowed( regex );
  }

}