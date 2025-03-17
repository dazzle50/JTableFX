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

package rjc.table.undo;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import rjc.table.Utils;
import rjc.table.view.Colours;

/*************************************************************************************************/
/*************** Control for viewing and interacting with undo-stack command list ****************/
/*************************************************************************************************/

public class UndoStackView extends Parent
{
  private int       m_height;           // view height
  private int       m_width;            // view width

  private UndoStack m_undostack;
  private Canvas    m_canvas;
  private ScrollBar m_scrollbar;

  private int       m_rowDescent;
  private int       m_rowHeight;
  private boolean   m_redrawIsRequested;

  /******************************************* resize ********************************************/
  @Override
  public void resize( double width, double height )
  {
    // resize the view
    m_width = (int) width;
    m_height = (int) height;
    layoutView();
  }

  /**************************************** isResizable ******************************************/
  @Override
  public boolean isResizable()
  {
    // view is resizable
    return true;
  }

  /***************************************** minHeight *******************************************/
  @Override
  public double minHeight( double width )
  {
    // view minimum height is zero
    return 0.0;
  }

  /****************************************** minWidth *******************************************/
  @Override
  public double minWidth( double height )
  {
    // view minimum width is zero
    return 0.0;
  }

  /***************************************** prefHeight ******************************************/
  @Override
  public double prefHeight( double width )
  {
    // view will take as much space as it can get so scroll-bars at edge of area
    return Integer.MAX_VALUE;
  }

  /****************************************** prefWidth ******************************************/
  @Override
  public double prefWidth( double height )
  {
    // view will take as much space as it can get so scroll-bars at edge of area
    return Integer.MAX_VALUE;
  }

  /****************************************** getWidth *******************************************/
  public int getWidth()
  {
    // return view width
    return m_width;
  }

  /****************************************** getHeight ******************************************/
  public int getHeight()
  {
    // return view height
    return m_height;
  }

  /**************************************** constructor ******************************************/
  public UndoStackView( UndoStack undostack )
  {
    // get default string bounds
    Bounds bounds = ( new Text( "Qwerty" ) ).getLayoutBounds();
    m_rowHeight = (int) Math.ceil( bounds.getHeight() );
    m_rowDescent = (int) Math.floor( -bounds.getMinY() );

    // create undo-stack view
    m_undostack = undostack;
    undostack.addListener( ( stack, msg ) -> layoutView() );
    visibleProperty().addListener( property -> layoutView() );

    // setup scroll bar
    m_scrollbar = new ScrollBar();
    m_scrollbar.setOrientation( Orientation.VERTICAL );
    m_scrollbar.setMinWidth( 18 );
    m_scrollbar.setVisible( false );
    m_scrollbar.valueProperty().addListener( property -> requestRedraw() );

    // setup canvas
    m_canvas = new Canvas();
    m_canvas.setFocusTraversable( true );
    m_canvas.focusedProperty().addListener( property -> requestRedraw() );
    m_canvas.setOnMousePressed( event -> setIndex( getIndexAtY( event.getY() ) + 1 ) );
    m_canvas.setOnMouseDragged( event -> setIndex( getIndexAtY( event.getY() ) + 1 ) );
    m_canvas.setOnScroll( event ->
    {
      // increase or decrease undo-stack index on mouse scroll event
      if ( event.getDeltaY() > 0 )
        setIndex( m_undostack.getIndex() - 1 );
      else
        setIndex( m_undostack.getIndex() + 1 );
    } );
    m_canvas.setOnKeyPressed( event ->
    {
      // update undo-stack index on key press event
      switch ( event.getCode() )
      {
        case HOME:
          setIndex( 0 );
          break;
        case END:
          setIndex( m_undostack.getSize() );
          break;
        case PAGE_UP:
          setIndex( m_undostack.getIndex() - (int) ( m_canvas.getHeight() / m_rowHeight ) );
          break;
        case PAGE_DOWN:
          setIndex( m_undostack.getIndex() + (int) ( m_canvas.getHeight() / m_rowHeight ) );
          break;
        case UP:
        case KP_UP:
          setIndex( m_undostack.getIndex() - 1 );
          break;
        case DOWN:
        case KP_DOWN:
          setIndex( m_undostack.getIndex() + 1 );
          break;
        case Z:
          if ( event.isControlDown() )
            setIndex( m_undostack.getIndex() - 1 );
          break;
        case Y:
          if ( event.isControlDown() )
            setIndex( m_undostack.getIndex() + 1 );
          break;
        default:
          break;
      }
    } );

    // add the canvas and scroll-bar to the view parent
    getChildren().addAll( m_canvas, m_scrollbar );
  }

  /******************************************* setIndex ******************************************/
  private void setIndex( int index )
  {
    // set undo-stack index
    index = Utils.clamp( index, 0, m_undostack.getSize() );
    m_undostack.setIndex( index );
  }

  /***************************************** layoutView ******************************************/
  private void layoutView()
  {
    // resize canvas and scroll-bar
    if ( isVisible() )
    {
      // set scroll bar to correct visibility
      double fullHeight = m_rowHeight * ( m_undostack.getSize() + 1 );
      boolean need = m_height < fullHeight;
      m_scrollbar.setVisible( need );

      // set scroll bars correct thumb size and position
      if ( need )
      {
        m_scrollbar.relocate( m_width - m_scrollbar.getWidth(), 0.0 );
        m_scrollbar.setPrefHeight( m_height );

        double max = fullHeight - m_height;
        m_scrollbar.setMax( max );
        m_scrollbar.setVisibleAmount( max * m_height / fullHeight );
        if ( m_scrollbar.getValue() > max )
          m_scrollbar.setValue( max );
      }
      else
        m_scrollbar.setValue( 0.0 );

      // set canvas to correct size to not overlap scroll bars
      m_canvas.setHeight( m_height );
      m_canvas.setWidth( m_width - ( need ? m_scrollbar.getWidth() : 0 ) );

      // ensure current index is visible
      int y = getYStart( m_undostack.getIndex() - 1 );
      if ( y < 0 )
        m_scrollbar.setValue( m_scrollbar.getValue() + y );
      else if ( y > m_canvas.getHeight() - m_rowHeight )
        m_scrollbar.setValue( m_scrollbar.getValue() + y - m_height + m_rowHeight );

      // request canvas is redrawn
      requestRedraw();
    }
  }

  /**************************************** requestRedraw ****************************************/
  private void requestRedraw()
  {
    // request redraw of undo-stack view
    if ( isVisible() && !m_redrawIsRequested )
    {
      m_redrawIsRequested = true;
      Platform.runLater( () -> redraw() );
    }
  }

  /******************************************** redraw *******************************************/
  private void redraw()
  {
    // redraw undo-stack canvas
    m_redrawIsRequested = false;
    GraphicsContext gc = m_canvas.getGraphicsContext2D();
    gc.setFontSmoothingType( FontSmoothingType.LCD );

    // fill background
    gc.setFill( Colours.TEXT_DEFAULT_BACKGROUND );
    gc.fillRect( 0.0, 0.0, m_canvas.getWidth(), m_canvas.getHeight() );

    // determine undo-stack visible range
    int min = getIndexAtY( 0.0 );
    int max = getIndexAtY( m_canvas.getHeight() );

    // draw undo-stack text
    String text;
    for ( int item = min; item <= max; item++ )
    {
      if ( item < 0 )
        text = "<empty>";
      else
        text = m_undostack.getText( item );

      // colour current index item differently
      int y = getYStart( item );
      if ( item == m_undostack.getIndex() - 1 )
      {
        if ( m_canvas.isFocused() )
          gc.setFill( Colours.TEXT_SELECTED_BACKGROUND );
        else
          gc.setFill( Colours.TEXT_SELECTED_BACKGROUND.desaturate().desaturate() );

        gc.fillRect( 0.0, y, getWidth(), m_rowHeight );
        gc.setFill( Colours.TEXT_SELECTED );
      }
      else
        gc.setFill( Colours.TEXT_DEFAULT );

      gc.fillText( text, 3.0, y + m_rowDescent );
    }
  }

  /****************************************** getYStart ******************************************/
  private int getYStart( int index )
  {
    // get start y-coordinate for index row on canvas
    return (int) ( ( index + 1 ) * m_rowHeight - m_scrollbar.getValue() );
  }

  /***************************************** getIndexAtY *****************************************/
  private int getIndexAtY( double y )
  {
    // get index at y-coordinate on canvas
    int row = (int) ( ( y + m_scrollbar.getValue() ) / m_rowHeight ) - 1;
    return Utils.clamp( row, -1, m_undostack.getSize() - 1 );
  }

}