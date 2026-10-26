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

import java.security.InvalidParameterException;

import rjc.table.control.ChooseField;

/*************************************************************************************************/
/************************** Table cell spin editor for choosing object ***************************/
/*************************************************************************************************/

/**
 * Table cell editor that allows selection of an object from a predefined set of values.
 * <p>
 * The editor uses a {@link ChooseField} as its control. String values that are not present in the
 * predefined list are tolerated by {@link #setValue(Object)}, while other invalid values result in
 * an {@link IllegalArgumentException}.
 */
public class EditorChoose extends AbstractCellEditor
{
  private ChooseField m_choose;

  /**************************************** constructor ******************************************/
  /**
   * Creates an editor containing the supplied selectable values.
   * Initially selects the first value with empty display text, or the first value if none has
   * empty display text. The array is copied, but its choice objects are shared.
   *
   * @param values selectable values
   * @throws NullPointerException if {@code values} is null
   * @throws IllegalArgumentException if {@code values} is empty
   */
  public EditorChoose( Object[] values )
  {
    // create spin table cell editor for integer
    m_choose = new ChooseField( values );
    setControl( m_choose );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the currently selected value.
   *
   * @return current selected choice object, which may be null
   */
  @Override
  public Object getValue()
  {
    // get editor current value
    return m_choose.getSelected();
  }

  /******************************************* setValue ******************************************/
  /**
   * Sets the editor's selected value.
   * <p>
   * Equal choices are selected first; otherwise strings are matched case-insensitively against
   * the start of other choices' display text, searching after the current selection and wrapping.
   * Strings with no match leave the selection unchanged. Null selects a null choice when one
   * exists, or the first choice otherwise. Other rejected values cause an
   * {@link IllegalArgumentException}.
   *
   * @param value value to select
   * @throws IllegalArgumentException if {@code value} is rejected and is not a string
   */
  @Override
  public void setValue( Object value )
  {
    // set editor current value
    try
    {
      m_choose.setSelected( value );
    }
    catch ( InvalidParameterException exception )
    {
      // value is not in the list of values or string, so throw exception
      if ( !( value instanceof String ) )
        throw new IllegalArgumentException( "Invalid value: " + value );
    }
  }
}