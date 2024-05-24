/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

/*************************************************************************************************/
/**************************** Generic spin control for number values *****************************/
/*************************************************************************************************/

public class NumberSpinField extends ButtonField
{
  private DecimalFormat m_numberFormat; // number decimal format

  /**************************************** constructor ******************************************/
  public NumberSpinField()
  {
    // set default spin editor characteristics
    setFormat( "0", 0 );
    setValue( getMin() );

    // add listener to remove any excess leading zeros
    textProperty().addListener( ( observable, oldText, newText ) ->
    {
      String text = getValue();
      if ( text.length() > 1 && text.charAt( 0 ) == '0' && Character.isDigit( text.charAt( 1 ) )
          && m_numberFormat.getMinimumIntegerDigits() == 1 )
        super.setValue( text.substring( 1 ) );
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
  public void setFormat( String format, int maxFractionDigits )
  {
    // check inputs
    if ( maxFractionDigits < 0 || maxFractionDigits > 8 )
      throw new IllegalArgumentException( "Digits after deciminal place out of 0-8 range! " + maxFractionDigits );

    // set number format
    m_numberFormat = new DecimalFormat( format );
    m_numberFormat.setMaximumFractionDigits( maxFractionDigits );
    setRange( getMin(), getMax() );
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
    // determine regular expression defining text allowed to be entered
    StringBuilder allow = new StringBuilder( 32 );
    allow.append( Pattern.quote( getPrefix() ) );

    if ( getMin() < 0.0 )
      allow.append( "-?" );
    allow.append( "\\d*" );
    if ( m_numberFormat != null && m_numberFormat.getMaximumFractionDigits() > 0 )
      allow.append( "\\.?\\d{0," + m_numberFormat.getMaximumFractionDigits() + "}" );

    allow.append( Pattern.quote( getSuffix() ) );
    setAllowed( allow.toString() );
  }
}
