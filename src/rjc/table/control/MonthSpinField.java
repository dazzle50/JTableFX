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

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.scene.input.KeyEvent;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/******************************** Spin control for picking month *********************************/
/*************************************************************************************************/

public class MonthSpinField extends ButtonField implements ISignal
{
  /**************************************** constructor ******************************************/
  public MonthSpinField()
  {
    // set default spin field characteristics
    setEditable( false );
    setRange( 1.0, 12.0 );
    setStepPage( 1.0, 3.0 );
    setValue( Month.JANUARY );
    setButtonType( ButtonType.UP_DOWN );

    // react to key typed
    setOnKeyTyped( event -> keyTyped( event ) );

    // signal when month changes
    textProperty().addListener( ( o, newT, oldT ) -> signal( getMonth() ) );
  }

  /****************************************** setValue *******************************************/
  @Override
  public void setValue( Object value )
  {
    // set value to month display name
    if ( value instanceof Month )
      super.setValue( getDisplayName( ( (Month) value ).getValue() ) );
    else if ( value instanceof Number )
      super.setValue( getDisplayName( ( (Number) value ).intValue() ) );
    else
      super.setValue( value );
  }

  /***************************************** getDouble *******************************************/
  @Override
  public double getDouble()
  {
    // attempt to get month index from field text, otherwise return min
    String text = getValue();
    for ( int index = 1; index <= 12; index++ )
      if ( text.equals( getDisplayName( index ) ) )
        return index;

    return getMin();
  }

  /****************************************** getMonth *******************************************/
  public Month getMonth()
  {
    // get month from spin control
    String text = getValue();
    for ( int index = 1; index <= 12; index++ )
      if ( text.equals( getDisplayName( index ) ) )
        return Month.of( index );

    return null;
  }

  /************************************** getDisplayName ****************************************/
  public String getDisplayName( int index )
  {
    // get month display name for specified index
    return Month.of( index ).getDisplayName( TextStyle.FULL, Locale.getDefault() );
  }

  /****************************************** keyTyped *******************************************/
  public void keyTyped( KeyEvent event )
  {
    // find next month that starts with typed key (case-insensitive)
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
