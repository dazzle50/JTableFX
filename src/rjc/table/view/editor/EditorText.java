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

import rjc.table.control.ExpandingField;

/*************************************************************************************************/
/******************************* Table cell editor for simple text *******************************/
/*************************************************************************************************/

/**
 * Table cell editor backed by an {@link ExpandingField}, returning its current text.
 * <p>
 * Values are converted with {@link Object#toString()}, with null displayed as an empty string.
 * An optional regular expression filters user text replacements. A non-null table status is
 * required when opening the editor.
 */
public class EditorText extends AbstractCellEditor
{
  private ExpandingField editor = new ExpandingField();

  /**************************************** constructor ******************************************/
  /**
   * Creates a text editor with empty text and no input filter.
   */
  public EditorText()
  {
    // create text table cell editor
    setControl( editor );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the text currently displayed by the editor.
   *
   * @return current string value
   */
  @Override
  public Object getValue()
  {
    // get editor text
    return editor.getText();
  }

  /******************************************* setValue ******************************************/
  /**
   * Displays the value's string representation, or an empty string for null.
   * Direct assignment does not apply the user-input filter set by {@link #setAllowed(String)}.
   *
   * @param value value to convert to text, or null for empty text
   */
  @Override
  public void setValue( Object value )
  {
    // set editor text
    String str = value == null ? "" : value.toString();
    editor.setText( str );
  }

  /****************************************** setAllowed *****************************************/
  /**
   * Sets a regular expression that must match the complete result of a user text replacement.
   * Existing text and directly assigned values are not checked against the expression.
   *
   * @param regex allowed full-text pattern, or null to disable filtering
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is not a valid regular expression
   */
  public void setAllowed( String regex )
  {
    // regular expression that limits what can be entered into editor
    editor.setAllowed( regex );
  }

}