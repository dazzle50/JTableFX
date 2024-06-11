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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/*************************************************************************************************/
/************************************ Date-time field control ************************************/
/*************************************************************************************************/

import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

public class DateTimeField extends ButtonField implements ISignal
{
  private DateTime m_datetime; // field current date-time (or most recent valid)

  /**************************************** constructor ******************************************/
  public DateTimeField( ObservableStatus status )
  {
    // construct field
    setStatus( status );
    setButtonType( ButtonType.DOWN );
    // TODO xxxxxxxxxxxxxxxxxxxxxxxx new DateTimeDropDown( this );

    // react to text changes, for example user typing new time
    textProperty().addListener( ( property, oldText, newText ) ->
    {
      try
      {
        // if no exception raised and date is different send signal (but don't update text)
        DateTime dt = DateTime.parse( newText,
            "[uuuuMMdd][uu-M-d][uuuu-M-d][uuuu-DDD][['T'][' '][HHmmss][HHmm][H:m:s][H:m][.SSS][.SS][.S]]" );
        if ( !dt.equals( m_datetime ) )
        {
          m_datetime = dt;
          signal( dt );
        }

        getStatus().update( Level.NORMAL, "Date-time: " + formatStatus( dt ) );
        setStyle( getStatus().getStyle() );
      }
      catch ( Exception exception )
      {
        getStatus().update( Level.ERROR, "Date-time format is not recognised" );
        setStyle( getStatus().getStyle() );
      }
    } );

    // react to focus change to ensure text shows date-time in correct format
    focusedProperty().addListener( ( property, oldF, newF ) ->
    {
      setText( format( m_datetime ) );
      positionCaret( getText().length() );
      getStatus().update( Level.NORMAL, null );
    } );

    // modify date-time if up or down arrows pressed
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.UP )
      {
        event.consume();
        changeValue( 1, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
      }

      if ( event.getCode() == KeyCode.DOWN )
      {
        event.consume();
        changeValue( -1, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
      }

      if ( event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE )
      {
        setText( format( m_datetime ) );
        positionCaret( getText().length() );
      }
    } );

    // set default date-time to now truncated to hour
    long now = DateTime.now().getMilliseconds() / 3600000L;
    setDateTime( new DateTime( now * 3600000L ) );
  }

  /**************************************** getDateTime ******************************************/
  public DateTime getDateTime()
  {
    // return field date-time (or most recent valid)
    return m_datetime;
  }

  /**************************************** setDateTime ******************************************/
  public void setDateTime( DateTime datetime )
  {
    // set current field date, display in text, signal change
    if ( !datetime.equals( m_datetime ) )
    {
      m_datetime = datetime;
      setText( format( datetime ) );
      positionCaret( getText().length() );
      signal( datetime );
    }
  }

  /******************************************* format ********************************************/
  public String format( DateTime datetime )
  {
    // return date-time in display format
    return datetime.toString( "yyyy-MM-dd HH:mm:ss.SSS" );
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( DateTime datetime )
  {
    // return date in status format
    return datetime.toString( "eeee d MMMM yyyy HH:mm:ss.SSS" );
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    if ( alt )
    {
      // modify field time-part
      if ( !shift && !ctrl )
        m_datetime.addMilliseconds( (int) ( delta * Time.ONE_HOUR ) );
      if ( shift && !ctrl )
        m_datetime.addMilliseconds( (int) ( delta * Time.ONE_MINUTE ) );
      if ( !shift && ctrl )
        m_datetime.addMilliseconds( (int) ( delta * Time.ONE_SECOND ) );

      setText( format( m_datetime ) );
      positionCaret( getText().length() );
      signal( m_datetime );
    }
    else
    {
      // modify field date-part
      if ( !shift && !ctrl )
        setDateTime( getDateTime().plusDays( (int) delta ) );
      if ( shift && !ctrl )
        setDateTime( getDateTime().plusMonths( (int) delta ) );
      if ( !shift && ctrl )
        setDateTime( getDateTime().plusYears( (int) delta ) );
    }
  }

}