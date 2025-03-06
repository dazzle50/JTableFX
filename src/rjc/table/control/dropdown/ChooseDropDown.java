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
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Screen;
import rjc.table.Utils;
import rjc.table.control.ButtonField;
import rjc.table.control.ChooseField;
import rjc.table.view.Colours;

/*************************************************************************************************/
/*************************** Pop-up window to support selecting choice ***************************/
/*************************************************************************************************/

public class ChooseDropDown extends Popup
{
  private ChooseField                   m_parent;
  private Canvas                        m_canvas;
  private ScrollBar                     m_scrollbar;
  private int                           m_rowHeight;
  private int                           m_rowDescent;
  private DropShadow                    m_shadow;

  private static final int              BORDER = 2;

  private static ChangeListener<Object> HIDE_LISTENER;

  /**************************************** constructor ******************************************/
  public ChooseDropDown( ChooseField parent )
  {
    // create pop-up window to display drop-down list
    m_parent = parent;
    m_canvas = new Canvas();
    m_scrollbar = new ScrollBar();
    getContent().addAll( m_canvas, m_scrollbar );

    // add shadow
    m_shadow = new DropShadow();
    m_shadow.setColor( Colours.SELECTED_BORDER );
    m_shadow.setRadius( 4.0 );
    getScene().getRoot().setEffect( m_shadow );

    // determine row height and row text descent
    Bounds bounds = ( new Text( "Qwerty" ) ).getLayoutBounds();
    m_rowHeight = (int) Math.ceil( bounds.getHeight() );
    m_rowDescent = (int) Math.floor( -bounds.getMinY() );

    // react to mouse events
    m_canvas.setOnMouseMoved( event -> redraw( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMousePressed( event -> m_parent.setSelectedIndex( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMouseDragged( event -> m_parent.setSelectedIndex( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMouseReleased( event -> hide() );
    m_canvas.setOnScroll( parent.SCROLL_HANDLER );

    // react to scroll-bar changes
    m_scrollbar.valueProperty().addListener( ( value ) -> redraw( m_parent.getSelectedIndex() ) );

    // toggle pop-up when parent is pressed or to hide when parent loses focus
    parent.getButton().setOnMousePressed( event -> toggle() );
    parent.focusedProperty().addListener( ( focus ) ->
    {
      if ( isShowing() && !m_parent.isFocused() )
        toggle();
    } );

    // create listener for hiding drop-down on window movement
    if ( HIDE_LISTENER == null )
      HIDE_LISTENER = ( o, oldV, newV ) -> toggle();
  }

  /******************************************* toggle ********************************************/
  public void toggle()
  {
    // if drop-down is showing, hide, otherwise show
    if ( isShowing() )
    {
      m_parent.getScene().getWindow().xProperty().removeListener( HIDE_LISTENER );
      m_parent.getScene().getWindow().yProperty().removeListener( HIDE_LISTENER );
      hide();
    }
    else
    {
      // location and default size - CANNOT do in drop-down constructor
      Point2D point = m_parent.localToScreen( 0.0, m_parent.getHeight() );
      double x = point.getX() - m_shadow.getRadius() + 1.0;
      double y = point.getY() - m_shadow.getRadius() + 1.0;
      int width = (int) m_parent.getWidth();
      int prefHeight = m_parent.getCount() * m_rowHeight + BORDER * 2;

      // determine if scroll-bar needed
      Screen screen = Screen.getScreensForRectangle( x, y, 0.0, 0.0 ).get( 0 );
      int maxHeight = (int) ( screen.getVisualBounds().getMaxY() - point.getY() - m_shadow.getRadius() );
      if ( prefHeight > maxHeight )
      {
        // scroll-bar needed
        m_canvas.setHeight( maxHeight );
        m_scrollbar.setVisible( true );
        m_scrollbar.setOrientation( Orientation.VERTICAL );
        m_scrollbar.setMax( prefHeight - maxHeight );
        m_scrollbar.setVisibleAmount( m_scrollbar.getMax() * maxHeight / prefHeight );
        m_scrollbar.setPrefHeight( maxHeight - 2 );
        m_scrollbar.setPrefWidth( ButtonField.BUTTONS_WIDTH_MAX );
        m_scrollbar.setLayoutX( width - ButtonField.BUTTONS_WIDTH_MAX - 1 );
        m_scrollbar.setLayoutY( 1 );
        m_scrollbar.setValue( 0 );
      }
      else
      {
        // no scroll-bar needed
        m_scrollbar.setVisible( false );
        m_canvas.setHeight( prefHeight );
      }
      m_canvas.setWidth( width );

      // show pop-up and redraw canvas
      show( m_parent, x, y );
      scrollToIndex( m_parent.getSelectedIndex() );

      // if parent window moves need to hide this drop-down
      m_parent.getScene().getWindow().xProperty().addListener( HIDE_LISTENER );
      m_parent.getScene().getWindow().yProperty().addListener( HIDE_LISTENER );
    }
  }

  /**************************************** scrollToIndex ****************************************/
  public void scrollToIndex( int index )
  {
    // adjust scroll-bar if necessary to ensure index is visible
    int h = (int) m_canvas.getHeight();
    if ( h > 0 )
    {
      // redraw canvas with parent selected index highlighted
      int y = getYStart( index ) - 2;
      if ( y < 0 )
        m_scrollbar.setValue( m_scrollbar.getValue() + y );
      else if ( y + 4 + m_rowHeight > h )
        m_scrollbar.setValue( m_scrollbar.getValue() - h + y + 4 + m_rowHeight );
      else
        redraw( index );
    }
  }

  /******************************************* redraw ********************************************/
  public void redraw( int index )
  {
    // redraw drop-down list contents onto canvas with specified item highlighted
    GraphicsContext gc = m_canvas.getGraphicsContext2D();
    double w = m_canvas.getWidth();
    double h = m_canvas.getHeight();
    gc.setFontSmoothingType( FontSmoothingType.LCD );

    // fill background
    gc.setFill( Colours.TEXT_DEFAULT_BACKGROUND );
    gc.fillRect( 0.0, 0.0, w, h );

    // determine list visible range
    int min = getIndexAtY( 0 );
    int max = getIndexAtY( (int) h );

    // draw list text
    int y = getYStart( min );
    for ( int item = min; item <= max; item++ )
    {
      // colour current selected index item differently
      if ( item == index )
      {
        gc.setFill( Colours.TEXT_SELECTED_BACKGROUND );
        gc.fillRect( BORDER, y, w - BORDER * 2, m_rowHeight );
        gc.setFill( Colours.TEXT_SELECTED );
      }
      else
        gc.setFill( Colours.TEXT_DEFAULT );

      gc.fillText( m_parent.getText( item ), BORDER + 3, y + m_rowDescent );
      y += m_rowHeight;
    }

    // draw border
    gc.setStroke( Colours.SELECTED_BORDER );
    gc.strokeRect( 0.5, 0.5, w - 1.0, h - 1.0 );
  }

  /**************************************** getIndexAtY ******************************************/
  private int getIndexAtY( int y )
  {
    // get index at y-coordinate on canvas
    int index = ( y + (int) m_scrollbar.getValue() - BORDER ) / m_rowHeight;
    return Utils.clamp( index, 0, m_parent.getCount() - 1 );
  }

  /****************************************** getYStart ******************************************/
  private int getYStart( int index )
  {
    // get start y-coordinate for index row on canvas
    return index * m_rowHeight - (int) m_scrollbar.getValue() + BORDER;
  }
}