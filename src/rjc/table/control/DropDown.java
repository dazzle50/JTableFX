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

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.FontSmoothingType;
import javafx.stage.Popup;
import rjc.table.view.Colours;

/*************************************************************************************************/
/***************************** Base class for control pop-up window ******************************/
/*************************************************************************************************/

public class DropDown extends Popup
{
  private ButtonField            m_parent;
  private Canvas                 m_canvas;
  private DropShadow             m_shadow;
  private GridPane               m_grid;

  private ChangeListener<Object> HIDE_LISTENER;

  public static final int        GRID_BORDER = 4;

  /**************************************** constructor ******************************************/
  public DropDown( ButtonField parent )
  {
    // create pop-up window with background canvas
    m_parent = parent;
    m_canvas = new Canvas();
    getContent().add( m_canvas );

    // add shadow
    m_shadow = new DropShadow();
    m_shadow.setColor( Colours.SELECTED_BORDER );
    m_shadow.setRadius( 4.0 );
    getScene().getRoot().setEffect( m_shadow );

    // prepare grid for layout
    m_grid = new GridPane();
    m_grid.setHgap( GRID_BORDER );
    m_grid.setVgap( GRID_BORDER );
    m_grid.setPadding( new Insets( GRID_BORDER ) );
    getContent().add( m_grid );

    // when grid size changes ensure background size matches
    m_grid.widthProperty().addListener( x -> setBackgroundSize( m_grid.getWidth(), m_grid.getHeight() ) );
    m_grid.heightProperty().addListener( x -> setBackgroundSize( m_grid.getWidth(), m_grid.getHeight() ) );

    // toggle pop-up when button is pressed
    parent.getButton().setOnMousePressed( event ->
    {
      event.consume();
      parent.requestFocus();
      toggle();
    } );

    // toggle pop-up when F2 pressed in parent or drop-down
    parent.addEventFilter( KeyEvent.KEY_PRESSED, event -> toggleOnF2( event ) );
    addEventFilter( KeyEvent.KEY_PRESSED, event -> toggleOnF2( event ) );

    // hide drop-down when parent (not button) pressed
    parent.setOnMousePressed( event -> hideDropDown() );

    // hide drop-down when parent loses focus
    parent.focusedProperty().addListener( ( observable, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        hideDropDown();
    } );

    // create listener for hiding drop-down on window movement
    HIDE_LISTENER = ( o, oldV, newV ) -> hideDropDown();
  }

  /****************************************** getGrid ********************************************/
  public GridPane getGrid()
  {
    // return grid-pane for layout
    return m_grid;
  }

  /******************************************* toggle ********************************************/
  public void toggle()
  {
    // if drop-down is showing, hide, otherwise show
    if ( isShowing() )
      hideDropDown();
    else
      showDropDown();
  }

  /***************************************** toggleOnF2 ******************************************/
  public void toggleOnF2( KeyEvent event )
  {
    // if F2 pressed and drop-down is showing, hide, otherwise show
    if ( event.getCode() == KeyCode.F2 )
    {
      event.consume();
      toggle();
    }
  }

  /**************************************** hideDropDown *****************************************/
  public void hideDropDown()
  {
    // ensure down-drop pop-up is hide (not showing)
    m_parent.getScene().getWindow().xProperty().removeListener( HIDE_LISTENER );
    m_parent.getScene().getWindow().yProperty().removeListener( HIDE_LISTENER );
    m_parent.setEditable( true );
    hide();
  }

  /**************************************** showDropDown *****************************************/
  public void showDropDown()
  {
    // if parent window moves need to hide this drop-down
    m_parent.getScene().getWindow().xProperty().addListener( HIDE_LISTENER );
    m_parent.getScene().getWindow().yProperty().addListener( HIDE_LISTENER );
    m_parent.setEditable( false );

    // ensure down-drop pop-up is showing (not hidden) - cannot determine in constructor
    Point2D point = m_parent.localToScreen( 0.0, m_parent.getHeight() );
    double x = point.getX() - m_shadow.getRadius() + 1.0;
    double y = point.getY() - m_shadow.getRadius() + 1.0;
    show( m_parent, x, y );
  }

  /************************************** setBackgroundSize **************************************/
  public void setBackgroundSize( double width, double height )
  {
    // set canvas size
    m_canvas.setWidth( width );
    m_canvas.setHeight( height );

    // paint background canvas with default fill and border
    GraphicsContext gc = m_canvas.getGraphicsContext2D();
    gc.setFontSmoothingType( FontSmoothingType.LCD );

    // fill background
    gc.setFill( Colours.HEADER_DEFAULT_FILL );
    gc.fillRect( 0.0, 0.0, width, height );

    // draw border
    gc.setStroke( Colours.SELECTED_BORDER );
    gc.strokeRect( 0.5, 0.5, width - 1.0, height - 1.0 );
  }

}