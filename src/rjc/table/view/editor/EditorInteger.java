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

import rjc.table.control.NumberSpinField;

/*************************************************************************************************/
/****************************** Table cell spin editor for integer *******************************/
/*************************************************************************************************/

/**
 * Table cell editor backed by a {@link NumberSpinField}, returning an {@link Integer} value.
 * <p>
 * Initially displays 1 with stepped range 1 to 999, format {@code "0"}, a 20-digit input limit,
 * and step/page increments of 1 and 10. The range clamps stepping; direct assignments are not
 * range-checked. A non-null table status is required when opening the editor.
 */
public class EditorInteger extends AbstractCellEditor
{
  private NumberSpinField m_spin = new NumberSpinField();

  /**************************************** constructor ******************************************/
  /**
   * Creates an integer editor using the default range, format, and step sizes.
   */
  public EditorInteger()
  {
    // create spin table cell editor for integer
    setControl( m_spin );
  }

  /******************************************* getValue ******************************************/
  /**
   * Returns the parsed double converted to an integer using Java's double-to-int conversion.
   * Invalid or blank text uses the field's {@code minimum - 1} sentinel before conversion.
   *
   * @return current {@link Integer}, or the converted invalid-value sentinel
   */
  @Override
  public Object getValue()
  {
    // get editor integer value
    return m_spin.getInteger();
  }

  /******************************************* setValue ******************************************/
  /**
   * Displays strings as raw text, null as blank text, and numbers using the configured format.
   * Direct assignment does not enforce the stepped range or user-input filter.
   *
   * @param value number, raw string, or null for blank text
   * @throws IllegalArgumentException if a non-null, non-string value cannot be formatted as a number
   */
  @Override
  public void setValue( Object value )
  {
    // set spin field value
    m_spin.setValue( value );
  }

  /****************************************** setFormat ******************************************/
  /**
   * Sets the decimal format and integer input limit, with no fractional digits.
   * Existing text is not reformatted. Formatting uses root-locale symbols with grouping disabled.
   *
   * @param format decimal format pattern
   * @param maxIntegerDigits maximum integer digits, from 0 to 99 inclusive
   * @throws IllegalArgumentException if the digit limit is outside its range or the pattern is malformed
   * @throws NullPointerException if {@code format} is null
   */
  public void setFormat( String format, int maxIntegerDigits )
  {
    // set number format
    m_spin.setFormat( format, maxIntegerDigits, 0 );
  }

  /******************************************* setRange ******************************************/
  /**
   * Sets the inclusive range used when stepping and rebuilds the user-input filter.
   * Existing text is unchanged; direct assignments are not range-checked.
   *
   * @param minValue minimum stepped value
   * @param maxValue maximum stepped value
   * @throws IllegalArgumentException if {@code minValue > maxValue}
   */
  public void setRange( int minValue, int maxValue )
  {
    // set range
    m_spin.setRange( minValue, maxValue );
  }

  /***************************************** setStepPage *****************************************/
  /**
   * Sets the increments used by arrow keys, the mouse wheel, button clicks, and page keys.
   *
   * @param step increment for arrow keys, the mouse wheel, and button clicks
   * @param page increment for Page Up and Page Down
   */
  public void setStepPage( int step, int page )
  {
    // set step and page increment/decrement sizes
    m_spin.setStepPage( step, page );
  }

  /*************************************** setPrefixSuffix ***************************************/
  /**
   * Sets text around the numeric value while preserving the current raw value text.
   *
   * @param prefix prefix text, or null for no prefix
   * @param suffix suffix text, or null for no suffix
   */
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // set prefix and suffix
    m_spin.setPrefixSuffix( prefix, suffix );
  }

}