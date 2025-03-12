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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.table.control.dropdown.DateDropDown;
import rjc.table.data.types.Date;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************************** Date field control ***************************************/
/*************************************************************************************************/

public class DateField extends ButtonField implements ISignal
{
  private Date m_date; // field current date (or most recent valid)

  /**************************************** constructor ******************************************/
  public DateField()
  {
    // construct field
    setButtonType( ButtonType.DOWN );
    new DateDropDown( this );

    // react to changes & key presses
    textProperty().addListener( ( property, oldText, newText ) -> parseText( newText ) );
    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );

    focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
        updateStatus( Level.NORMAL ); // gained focus
      else
        validText(); // lost focus
    } );

    // set default date to today
    setDate( Date.now() );
  }

  /****************************************** getDate ********************************************/
  public Date getDate()
  {
    // return current field date (or most recent valid)
    return m_date;
  }

  /****************************************** setDate ********************************************/
  public void setDate( Object value )
  {
    // set field value depending on object type
    if ( value instanceof Date date )
      setDate( date );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
      positionCaret( getText().length() );
    }
  }

  /****************************************** setDate ********************************************/
  public void setDate( Date date )
  {
    // set current field date, display in text, signal change
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      setText( format( date ) );
      positionCaret( getText().length() );
      signal( date );
    }
  }

  /******************************************* format ********************************************/
  private String format( Date date )
  {
    // return date in display format
    return date.toString();
  }

  /**************************************** formatStatus *****************************************/
  private String formatStatus( Date date )
  {
    // return date in status format
    return date.toString( "eeee d MMMM yyyy" );
  }

  /***************************************** parseText *******************************************/
  private void parseText( String newText )
  {
    // check if string can be parsed as a date, and update status
    try
    {
      // if no exception raised and date is different send signal (but don't update text)
      Date date = Date.parse( newText, "uuuu-MM-dd" );
      if ( !date.equals( m_date ) )
      {
        m_date = date;
        signal( date );
      }

      updateStatus( Level.NORMAL );
    }
    catch ( Exception exception )
    {
      updateStatus( Level.ERROR );
    }

  }

  /**************************************** updateStatus *****************************************/
  private void updateStatus( Level level )
  {
    // if focused, update status with level and appropriate text
    if ( getStatus() != null && focusWithinProperty().get() )
    {
      String msg = level == Level.NORMAL ? "Date: " + formatStatus( m_date ) : "Date format is not recognised";
      getStatus().update( level, msg );
    }

    // set style based on severity level
    setStyle( ObservableStatus.getStyle( level ) );
  }

  /***************************************** validText *******************************************/
  private void validText()
  {
    // ensure field displays last valid date
    setText( format( m_date ) );
    positionCaret( getText().length() );
    getStatus().clear();
  }

  /***************************************** keyPressed ******************************************/
  @Override
  public void keyPressed( KeyEvent event )
  {
    // react to certain key presses
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

    if ( event.getCode() == KeyCode.ESCAPE )
      validText();
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // modify field value
    if ( !shift && !ctrl )
      setDate( getDate().plusDays( (int) delta ) );
    if ( shift && !ctrl )
      setDate( getDate().plusMonths( (int) delta ) );
    if ( !shift && ctrl )
      setDate( getDate().plusYears( (int) delta ) );
  }

}