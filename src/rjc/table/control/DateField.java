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
import javafx.scene.input.ScrollEvent;
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
  public DateField( ObservableStatus status )
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
        Date date = Date.parse( newText, "uuuu-MM-dd" );
        if ( !date.equals( m_date ) )
        {
          m_date = date;
          signal( date );
        }

        getStatus().update( Level.NORMAL, "Date: " + format( date ) );
        setStyle( getStatus().getStyle() );
      }
      catch ( Exception exception )
      {
        getStatus().update( Level.ERROR, "Date format is not recognised" );
        setStyle( getStatus().getStyle() );
      }
    } );

    // react to focus change to ensure text shows date in correct format
    focusedProperty().addListener( ( property, oldF, newF ) ->
    {
      setText( format( m_date ) );
      positionCaret( getText().length() );
      getStatus().update( Level.NORMAL, null );
    } );

    // react to key presses
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.UP )
      {
        event.consume();
        step( 1, event.isShiftDown(), event.isControlDown() );
      }

      if ( event.getCode() == KeyCode.DOWN )
      {
        event.consume();
        step( -1, event.isShiftDown(), event.isControlDown() );
      }

      if ( event.getCode() == KeyCode.ENTER )
      {
        setText( format( m_date ) );
        positionCaret( getText().length() );
      }
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
  public String format( Date date )
  {
    // return date in display format
    return date.toString();
  }

  /******************************************** step ********************************************/
  public void step( int delta, boolean shift, boolean ctrl )
  {
    // modify field value
    if ( !shift && !ctrl )
      setDate( getDate().plusDays( delta ) );
    if ( shift && !ctrl )
      setDate( getDate().plusMonths( delta ) );
    if ( !shift && ctrl )
      setDate( getDate().plusYears( delta ) );
  }

  /**************************************** mouseScroll ******************************************/
  public void mouseScroll( ScrollEvent event )
  {
    // increment or decrement date depending on mouse wheel scroll event
    int delta = event.getDeltaY() > 0 ? 1 : -1;
    event.consume();
    step( delta, event.isShiftDown(), event.isControlDown() );
  }

}