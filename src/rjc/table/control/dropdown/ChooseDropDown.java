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

package rjc.table.control.dropdown;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import rjc.table.Utils;
import rjc.table.control.ButtonField;
import rjc.table.control.ChooseField;
import rjc.table.view.Colours;

/*************************************************************************************************/
/*************************** Pop-up window to support selecting choice ***************************/
/*************************************************************************************************/

public class ChooseDropDown extends Popup
{
  private ChooseField            m_parent;
  private Canvas                 m_canvas;
  private ScrollBar              m_scrollbar;
  private int                    m_rowHeight;
  private int                    m_rowDescent;
  private DropShadow             m_shadow;

  private static final int       BORDER         = 2;

  private Window                 m_observedWindow;
  private ChangeListener<Number> m_hideListener = ( property, oldValue, newValue ) -> hideDropDown();

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

    // configure fixed canvas and scrollbar characteristics
    GraphicsContext gc = m_canvas.getGraphicsContext2D();
    gc.setFont( m_parent.getFont() );
    gc.setFontSmoothingType( FontSmoothingType.LCD );
    m_scrollbar.setOrientation( Orientation.VERTICAL );
    m_scrollbar.setMin( 0.0 );
    m_scrollbar.setUnitIncrement( m_rowHeight );
    m_scrollbar.setPrefWidth( ButtonField.BUTTONS_WIDTH_MAX );
    m_scrollbar.setLayoutY( 1 );

    // react to mouse events
    m_canvas.setOnMouseMoved( event -> redraw( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMousePressed( event -> m_parent.setSelectedIndex( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMouseDragged( event -> m_parent.setSelectedIndex( getIndexAtY( (int) event.getY() ) ) );
    m_canvas.setOnMouseReleased( event -> hideDropDown() );
    m_canvas.setOnScroll( parent.SCROLL_HANDLER );

    // react to scroll-bar changes
    m_scrollbar.valueProperty().addListener( ( value ) -> redraw( m_parent.getSelectedIndex() ) );

    // toggle pop-up when parent is pressed or to hide when parent loses focus
    parent.getButton().setOnMousePressed( event ->
    {
      event.consume();
      parent.requestFocus();
      toggle();
    } );
    parent.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        hideDropDown();
    } );

    // ensure movement listeners are removed however the pop-up is hidden
    setOnHidden( event -> detachWindowListeners() );
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

  /*************************************** showDropDown ******************************************/
  private void showDropDown()
  {
    // only show when parent is attached to a window and has choices
    Scene scene = m_parent.getScene();
    Window window = scene == null ? null : scene.getWindow();
    if ( window == null || m_parent.getCount() <= 0 )
      return;

    // location and default size cannot be determined in constructor
    Point2D top = m_parent.localToScreen( 0.0, 0.0 );
    Point2D bottom = m_parent.localToScreen( 0.0, m_parent.getHeight() );
    if ( top == null || bottom == null )
      return;

    int width = (int) Math.ceil( m_parent.getWidth() );
    int prefHeight = m_parent.getCount() * m_rowHeight + BORDER * 2;
    if ( width <= 0 || prefHeight <= 0 )
      return;

    // select screen containing parent, falling back to primary screen
    var screens = Screen.getScreensForRectangle( bottom.getX(), bottom.getY(), 1.0, 1.0 );
    Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get( 0 );
    Rectangle2D visual = screen.getVisualBounds();

    // constrain width and choose side with sufficient/more available height
    width = Math.min( width, (int) visual.getWidth() );
    double below = visual.getMaxY() - bottom.getY() - m_shadow.getRadius();
    double above = top.getY() - visual.getMinY() - m_shadow.getRadius();
    boolean openBelow = prefHeight <= below || below >= above;
    int availableHeight = (int) Math.floor( Math.max( 0.0, openBelow ? below : above ) );
    int height = Math.min( prefHeight, availableHeight );
    if ( width <= 0 || height <= 0 )
      return;

    // align canvas with parent while accounting for shadow around pop-up root
    double contentX = Utils.clamp( bottom.getX(), visual.getMinX(), visual.getMaxX() - width );
    double x = contentX - m_shadow.getRadius() + 1.0;
    double y = ( openBelow ? bottom.getY() : top.getY() - height ) - m_shadow.getRadius() + 1.0;

    // size canvas and configure scrollbar if content does not fit
    m_canvas.setWidth( width );
    m_canvas.setHeight( height );
    if ( prefHeight > height )
    {
      m_scrollbar.setVisible( true );
      m_scrollbar.setMax( prefHeight - height );
      m_scrollbar.setVisibleAmount( height );
      m_scrollbar.setBlockIncrement( height );
      m_scrollbar.setPrefHeight( Math.max( 0, height - 2 ) );
      m_scrollbar.setLayoutX( width - ButtonField.BUTTONS_WIDTH_MAX - 1 );
      m_scrollbar.setValue( 0.0 );
    }
    else
    {
      m_scrollbar.setVisible( false );
      m_scrollbar.setMax( 0.0 );
      m_scrollbar.setValue( 0.0 );
    }

    // show, redraw, and observe owner movement only while visible
    show( m_parent, x, y );
    attachWindowListeners( window );
    scrollToIndex( m_parent.getSelectedIndex() );
  }

  /*************************************** hideDropDown ******************************************/
  private void hideDropDown()
  {
    // always release listeners before hiding
    detachWindowListeners();
    if ( isShowing() )
      hide();
  }

  /************************************ attachWindowListeners ************************************/
  private void attachWindowListeners( Window window )
  {
    // hide this drop-down if its owner window moves
    detachWindowListeners();
    m_observedWindow = window;
    window.xProperty().addListener( m_hideListener );
    window.yProperty().addListener( m_hideListener );
  }

  /************************************ detachWindowListeners ************************************/
  private void detachWindowListeners()
  {
    // release movement listeners from previously observed owner
    if ( m_observedWindow == null )
      return;

    m_observedWindow.xProperty().removeListener( m_hideListener );
    m_observedWindow.yProperty().removeListener( m_hideListener );
    m_observedWindow = null;
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
