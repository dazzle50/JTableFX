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

package rjc.table.control.dropdown;

import javafx.beans.value.ChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import rjc.table.Utils;
import rjc.table.control.ButtonField;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/********************* Abstract control that has a drop-down & sends signals *********************/
/*************************************************************************************************/

abstract public class AbstractDropDownField extends ButtonField implements ISignal
{
  private DropDown               m_dropdown;   // drop-down associated with this field

  private ChangeListener<Object> HIDE_LISTENER;

  /**************************************** constructor ******************************************/
  public AbstractDropDownField()
  {
    // construct field
    setButtonType( ButtonType.DOWN );

    // react to changes & key presses
    textProperty().addListener( ( property, oldText, newText ) -> checkText( newText ) );
    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );

    // react to focus changes
    focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
        updateStatus( Level.INFO ); // gained focus
      else
        validText(); // lost focus
    } );

    // construct drop-down
    createDropDown();
  }

  /*************************************** createDropDown ****************************************/
  private void createDropDown()
  {
    // create drop-down and add appropriate listeners
    m_dropdown = new DropDown();

    // toggle pop-up when button is pressed
    getButton().setOnMousePressed( event ->
    {
      event.consume();
      requestFocus();
      updateDropDownWidgets();
      m_dropdown.toggle( this );
    } );

    // hide drop-down when field (not button) pressed
    setOnMousePressed( event -> m_dropdown.hide( this ) );

    // toggle pop-up when F2 pressed in field or drop-down
    addEventFilter( KeyEvent.KEY_PRESSED, event -> toggleOnF2( event ) );
    m_dropdown.addEventFilter( KeyEvent.KEY_PRESSED, event -> toggleOnF2( event ) );

    // hide drop-down when field loses focus
    focusedProperty().addListener( ( proprerty, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        m_dropdown.hide( this );
    } );

    // if parent window moves need to hide this drop-down
    HIDE_LISTENER = ( o, oldV, newV ) -> m_dropdown.hide( this );
    sceneProperty().addListener( ( property, oldScene, newScene ) ->
    {
      if ( newScene == null )
      {
        oldScene.getWindow().xProperty().removeListener( HIDE_LISTENER );
        oldScene.getWindow().yProperty().removeListener( HIDE_LISTENER );
      }
      else
      {
        newScene.getWindow().xProperty().addListener( HIDE_LISTENER );
        newScene.getWindow().yProperty().addListener( HIDE_LISTENER );
      }
    } );
  }

  /*************************************** getDropDownGrid ***************************************/
  protected GridPane getDropDownGrid()
  {
    // return drop-down grid-pane
    return m_dropdown.getGrid();
  }

  /***************************************** toggleOnF2 ******************************************/
  private void toggleOnF2( KeyEvent event )
  {
    // if F2 pressed and drop-down is showing, hide, otherwise show
    if ( event.getCode() == KeyCode.F2 )
    {
      event.consume();
      updateDropDownWidgets();
      m_dropdown.toggle( this );
    }
  }

  /***************************************** checkText *******************************************/
  private void checkText( String text )
  {
    // check if string can be parsed, and update status depending if parsing successful
    try
    {
      parseText( Utils.clean( text ) );
      updateStatus( Level.INFO );
    }
    catch ( Exception exception )
    {
      updateStatus( Level.ERROR );
    }
  }

  /**************************************** updateStatus *****************************************/
  protected void updateStatus( Level level )
  {
    // if focused, update status with level and appropriate text
    if ( getStatus() != null && focusWithinProperty().get() )
      getStatus().update( level, statusText( level ) );

    // set style based on severity level
    setStyle( ObservableStatus.getStyle( level ) );
  }

  /***************************************** parseText *******************************************/
  abstract protected void parseText( String text );

  /***************************************** statusText ******************************************/
  abstract protected String statusText( Level level );

  /***************************************** validText *******************************************/
  abstract protected void validText();

  /************************************ updateDropDownWidgets ************************************/
  abstract protected void updateDropDownWidgets();

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
}
