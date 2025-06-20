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

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.text.FontSmoothingType;
import javafx.stage.Popup;
import rjc.table.control.ButtonField;
import rjc.table.view.Colours;

/*************************************************************************************************/
/***************************** Base class for control pop-up window ******************************/
/*************************************************************************************************/

public class DropDown extends Popup
{
  private Canvas          m_canvas;
  private DropShadow      m_shadow;
  private GridPane        m_grid;

  public static final int GRID_BORDER = 4;

  /**************************************** constructor ******************************************/
  public DropDown()
  {
    // create pop-up window with contents canvas
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
  }

  /****************************************** getGrid ********************************************/
  public GridPane getGrid()
  {
    // return grid-pane for layout
    return m_grid;
  }

  /******************************************* toggle ********************************************/
  public void toggle( ButtonField field )
  {
    // if drop-down is showing, hide, otherwise show
    if ( isShowing() )
      hide( field );
    else
      show( field );
  }

  /******************************************** hide *********************************************/
  public void hide( ButtonField field )
  {
    // hide drop-down and set field to editable
    field.setEditable( true );
    hide();
  }

  /******************************************** show *********************************************/
  public void show( ButtonField field )
  {
    // show drop-down and set field to non-editable
    field.setEditable( false );

    // determine drop-down location and show - cannot determine in constructor
    Point2D point = field.localToScreen( 0.0, field.getHeight() );
    double x = point.getX() - m_shadow.getRadius() + 1.0;
    double y = point.getY() - m_shadow.getRadius() + 1.0;
    show( field, x, y );
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