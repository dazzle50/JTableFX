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

package rjc.table.control;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************************** Generic spin control for number values *****************************/
/*************************************************************************************************/

public class NumberSpinField extends ButtonField implements ISignal
{
  private DecimalFormat m_numberFormat; // number decimal format
  private double        m_lastSignal;   // last value signalled to avoid signals when no change

  /**************************************** constructor ******************************************/
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
  @Override
  public void setValue( Object value )
  {
    // if string then set as specified, otherwise use number formatter
    if ( value instanceof String )
      super.setValue( value );
    else
      super.setValue( m_numberFormat.format( value ) );
  }

  /***************************************** getInteger ******************************************/
  public int getInteger()
  {
    // return field value as integer
    return (int) getDouble();
  }

  /****************************************** setFormat ******************************************/
  public void setFormat( String format, int maxIntegerDigits, int maxFractionDigits )
  {
    // check inputs
    if ( maxFractionDigits < 0 || maxFractionDigits > 8 )
      throw new IllegalArgumentException( "Digits after decimal place out of 0-8 range! " + maxFractionDigits );
    if ( maxIntegerDigits < 0 || maxIntegerDigits > 99 )
      throw new IllegalArgumentException( "Digits before decimal place out of 0-99 range! " + maxIntegerDigits );

    // set number format
    m_numberFormat = new DecimalFormat( format );
    m_numberFormat.setMaximumIntegerDigits( maxIntegerDigits );
    m_numberFormat.setMaximumFractionDigits( maxFractionDigits );
    determineAllowed();
  }

  /******************************************* setRange ******************************************/
  @Override
  public void setRange( double minValue, double maxValue )
  {
    // set range and number of digits after decimal point
    super.setRange( minValue, maxValue );
    determineAllowed();
  }

  /*************************************** setPrefixSuffix ***************************************/
  @Override
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // set prefix and suffix, translating null to ""
    super.setPrefixSuffix( prefix, suffix );
    determineAllowed();
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
      allow.append( "\\.?\\d{0," + m_numberFormat.getMaximumFractionDigits() + "}" );

    allow.append( Pattern.quote( getSuffix() ) );
    setAllowed( allow.toString() );
  }
}
