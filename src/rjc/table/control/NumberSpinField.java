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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************************** Generic spin control for number values *****************************/
/*************************************************************************************************/

/**
 * Numeric {@link ButtonField} that formats, filters, and steps number values.
 * <p>
 * Formatting uses {@link Locale#ROOT} symbols with grouping disabled, so the decimal separator is
 * always {@code .}. This matches the allowed-text pattern and parsing used by {@link #getDouble()}.
 * <p>
 * The field emits an {@link ISignal} message containing the current double value whenever the
 * parsed value changes. Blank or invalid text parses as a sentinel value below the configured
 * minimum.
 */
public class NumberSpinField extends ButtonField implements ISignal
{
  private DecimalFormat m_numberFormat; // number decimal format
  private double        m_lastSignal;   // last value signalled to avoid signals when no change

  /**************************************** constructor ******************************************/
  /**
   * Creates an integer spin field with range 1 to 999, format {@code "0"}, and an up/down button.
   */
  public NumberSpinField()
  {
    // set default spin editor characteristics
    setFormat( "0", 20, 0 );
    setValue( getMin() );
    setButtonType( ButtonType.UP_DOWN );

    // add listener to remove any excess leading zeros
    textProperty().addListener( ( property, oldText, newText ) ->
    {
      String text = getValue();
      if ( text.length() > 1 && text.charAt( 0 ) == '0' && Character.isDigit( text.charAt( 1 ) )
          && m_numberFormat.getMinimumIntegerDigits() == 1 )
        super.setValue( text.substring( 1 ) );

      // emit signal & clear status when number changes
      double num = getDouble();
      if ( num != m_lastSignal )
      {
        setStyle( ObservableStatus.getStyle( Level.INFO ) );
        if ( getStatus() != null )
          getStatus().clear();
        signal( num );
        m_lastSignal = num;
      }
    } );
  }

  /****************************************** setValue *******************************************/
  /**
   * Sets the displayed value.
   * <p>
   * Strings are used as raw text, null is displayed as blank text, and other values are formatted
   * with the current {@link DecimalFormat}.
   *
   * @param value value to display
   */
  @Override
  public void setValue( Object value )
  {
    // if string then set as specified, otherwise use number formatter
    if ( value == null )
      super.setValue( "" );
    else if ( value instanceof String )
      super.setValue( value );
    else
      super.setValue( m_numberFormat.format( value ) );
  }

  /***************************************** getInteger ******************************************/
  /**
   * Returns the current value truncated to an integer.
   *
   * @return current value as an integer, or the invalid-value sentinel truncated to an integer
   */
  public int getInteger()
  {
    // return field value as integer
    return (int) getDouble();
  }

  /****************************************** setFormat ******************************************/
  /**
   * Sets the numeric display format and the maximum digits accepted by the input filter.
   * <p>
   * Existing text is not reformatted. The regular expression used by {@link #setAllowed(String)} is
   * rebuilt from the digit limits, current range, and current prefix/suffix.
   *
   * @param format            decimal format pattern
   * @param maxIntegerDigits  maximum digits before the decimal point
   * @param maxFractionDigits maximum digits after the decimal point
   * @throws IllegalArgumentException if either digit limit is outside the supported range
   */
  public void setFormat( String format, int maxIntegerDigits, int maxFractionDigits )
  {
    // check inputs
    if ( maxFractionDigits < 0 || maxFractionDigits > 8 )
      throw new IllegalArgumentException( "Digits after decimal place out of 0-8 range! " + maxFractionDigits );
    if ( maxIntegerDigits < 0 || maxIntegerDigits > 99 )
      throw new IllegalArgumentException( "Digits before decimal place out of 0-99 range! " + maxIntegerDigits );

    // set number format
    m_numberFormat = new DecimalFormat( format, DecimalFormatSymbols.getInstance( Locale.ROOT ) );
    m_numberFormat.setGroupingUsed( false );
    m_numberFormat.setMaximumIntegerDigits( maxIntegerDigits );
    m_numberFormat.setMaximumFractionDigits( maxFractionDigits );
    determineAllowed();
  }

  /******************************************* setRange ******************************************/
  /**
   * Sets the numeric range and rebuilds the allowed-text pattern.
   *
   * @param minValue minimum allowed stepped value
   * @param maxValue maximum allowed stepped value
   * @throws IllegalArgumentException if minValue is greater than maxValue
   */
  @Override
  public void setRange( double minValue, double maxValue )
  {
    // set range and rebuild allowed text
    super.setRange( minValue, maxValue );
    determineAllowed();
  }

  /*************************************** setPrefixSuffix ***************************************/
  /**
   * Sets the prefix and suffix while preserving the current raw value.
   *
   * @param prefix prefix text, or null for no prefix
   * @param suffix suffix text, or null for no suffix
   */
  @Override
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // preserve current raw value while changing prefix & suffix
    String value = getValue();
    super.setPrefixSuffix( prefix, suffix );
    determineAllowed();
    super.setValue( value );
  }

  /************************************** determineAllowed ***************************************/
  private void determineAllowed()
  {
    // only determine if number format available
    if ( m_numberFormat == null )
      return;

    // determine regular expression defining text allowed to be entered
    StringBuilder allow = new StringBuilder( 32 );
    allow.append( Pattern.quote( getPrefix() ) );

    if ( getMin() < 0.0 )
      allow.append( "-?" );

    allow.append( "\\d{0," + m_numberFormat.getMaximumIntegerDigits() + "}" );

    if ( m_numberFormat.getMaximumFractionDigits() > 0 )
      allow.append( "(\\.\\d{0," + m_numberFormat.getMaximumFractionDigits() + "})?" );

    allow.append( Pattern.quote( getSuffix() ) );
    setAllowed( allow.toString() );
  }
}
