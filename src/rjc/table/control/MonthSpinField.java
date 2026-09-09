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

package rjc.table.control;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.scene.input.KeyEvent;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/******************************** Spin control for picking month *********************************/
/*************************************************************************************************/

/**
 * Non-editable spin field for selecting a month using names from the default locale.
 * <p>
 * Arrow keys and the button step through January to December, wrapping through any configured
 * overflow field. Typing selects the next month whose display name begins with that character.
 * Changes emit an {@link ISignal} containing the selected {@link Month}.
 */
public class MonthSpinField extends ButtonField implements ISignal
{
  /**************************************** constructor ******************************************/
  /**
   * Creates a month field initially selecting January.
   */
  public MonthSpinField()
  {
    // month names are selected programmatically, so direct text editing is disabled
    setEditable( false );
    setRange( 1.0, 12.0 );
    setStepPage( 1.0, 3.0 );
    setValue( Month.JANUARY );
    setButtonType( ButtonType.UP_DOWN );

    // allow fast selection by the first character of a localized month name
    setOnKeyTyped( event -> keyTyped( event ) );

    // emit the selected month whenever its display text changes
    textProperty().addListener( ( property, oldText, newText ) -> signal( getMonth() ) );
  }

  /****************************************** setValue *******************************************/
  /**
   * Displays a month supplied as a {@link Month}, a numeric month index, or literal text.
   * Numeric values are converted with {@link Number#intValue()}, and therefore discard any
   * fractional part. A null value is displayed as the literal text {@code "null"} by the base
   * field implementation.
   *
   * @param value month, one-based month number, text to display, or null
   * @throws java.time.DateTimeException if a numeric value is outside 1 to 12
   */
  @Override
  public void setValue( Object value )
  {
    // normalize supported value types to the localized display name
    if ( value instanceof Month month )
      super.setValue( getDisplayName( month.getValue() ) );
    else if ( value instanceof Number num )
      super.setValue( getDisplayName( num.intValue() ) );
    else
      super.setValue( value );
  }

  /***************************************** getDouble *******************************************/
  /**
   * Returns the one-based index of the displayed month.
   *
   * @return month index from 1 to 12, or the configured minimum when the text is not a month name
   */
  @Override
  public double getDouble()
  {
    // match localized names rather than attempting numeric parsing in ButtonField
    String text = getValue();
    for ( int index = 1; index <= 12; index++ )
      if ( text.equals( getDisplayName( index ) ) )
        return index;

    return getMin();
  }

  /****************************************** getMonth *******************************************/
  /**
   * Returns the month represented by the displayed text.
   *
   * @return selected month, or null when the text is not a recognised localized month name
   */
  public Month getMonth()
  {
    // return null for literal text supplied through setValue that is not a month name
    String text = getValue();
    for ( int index = 1; index <= 12; index++ )
      if ( text.equals( getDisplayName( index ) ) )
        return Month.of( index );

    return null;
  }

  /************************************** getDisplayName ****************************************/
  /**
   * Returns the full localized name of a one-based month index.
   *
   * @param index month index from 1 to 12
   * @return localized full month name
   * @throws java.time.DateTimeException if {@code index} is outside 1 to 12
   */
  public String getDisplayName( int index )
  {
    // use the locale current at lookup time; existing field text is not automatically reformatted
    return Month.of( index ).getDisplayName( TextStyle.FULL, Locale.getDefault() );
  }

  /****************************************** keyTyped *******************************************/
  /**
   * Selects the next month whose localized name starts with the typed character, ignoring case.
   *
   * @param event typed-key event
   * @throws NullPointerException if the current field text is not a recognised month name
   */
  public void keyTyped( KeyEvent event )
  {
    // begin after the recognised current month and wrap once through the calendar year
    String key = event.getCharacter().toLowerCase();
    int index = getMonth().getValue();
    for ( int delta = 0; delta < 11; delta++ )
    {
      index = index % 12 + 1;
      if ( getDisplayName( index ).toLowerCase().startsWith( key ) )
      {
        setValue( getDisplayName( index ) );
        return;
      }
    }
  }

}
