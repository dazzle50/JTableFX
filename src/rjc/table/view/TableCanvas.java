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

package rjc.table.view;

import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************** Canvas showing the table headers & body cells + BLANK excess space ***************/
/*************************************************************************************************/

public class TableCanvas extends TableCanvasDraw
{
  private TableView m_view;

  private TableAxis m_columnsAxis; // columns (horizontal) axis
  private TableAxis m_rowsAxis;    // rows (vertical) axis

  /**************************************** constructor ******************************************/
  public TableCanvas( TableView view )
  {
    super( view );
    m_view = view;

    // construct the table-axis
    m_columnsAxis = new TableAxis( m_view.getData().columnCountProperty() );
    m_rowsAxis = new TableAxis( m_view.getData().rowCountProperty() );

    // when canvas size changes draw new areas
    widthProperty().addListener( ( observable, oldW, newW ) -> widthChange( oldW.intValue(), newW.intValue() ) );
    heightProperty().addListener( ( observable, oldH, newH ) -> heightChange( oldH.intValue(), newH.intValue() ) );

    // ensure canvas visibility is same as parent table-view
    visibleProperty().bind( view.visibleProperty() );
  }

  /***************************************** widthChange *****************************************/
  public void widthChange( int oldWidth, int newWidth )
  {
    // only need to draw if new width is larger than old width
    if ( newWidth > oldWidth && isVisible() && oldWidth < m_columnsAxis.getTotalPixels() && getHeight() > 0.0 )
    {
      // clear background (+0.5 needed so anti-aliasing doesn't impact previous column)
      getGraphicsContext2D().clearRect( oldWidth + 0.5, 0.0, newWidth, getHeight() );

      // calculate which columns need to be redrawn
      int minColumn = m_view.getColumnIndex( oldWidth );
      if ( minColumn <= HEADER )
        minColumn = m_view.getColumnIndex( m_columnsAxis.getHeaderPixels() );
      int maxColumn = m_view.getColumnIndex( newWidth );
      redrawColumnsNow( minColumn, maxColumn );

      // check if row header needs to be redrawn
      if ( oldWidth < m_columnsAxis.getHeaderPixels() )
        redrawColumnNow( HEADER );

      // draw table overlay
      getOverlay().redrawNow();
    }
  }

  /**************************************** heightChange *****************************************/
  public void heightChange( int oldHeight, int newHeight )
  {
    // only need to draw if new height is larger than old height
    if ( newHeight > oldHeight && isVisible() && oldHeight < m_rowsAxis.getTotalPixels() && getWidth() > 0.0 )
    {
      // clear background
      getGraphicsContext2D().clearRect( 0.0, oldHeight + 0.5, getWidth(), newHeight );

      // calculate which rows need to be redrawn, and redraw them
      int minRow = m_view.getRowIndex( oldHeight );
      if ( minRow <= HEADER )
        minRow = m_view.getRowIndex( m_rowsAxis.getHeaderPixels() );
      int maxRow = m_view.getRowIndex( newHeight );
      redrawRowsNow( minRow, maxRow );

      // check if column header needs to be redrawn
      if ( oldHeight < m_rowsAxis.getHeaderPixels() )
        redrawRowNow( HEADER );

      // draw table overlay
      getOverlay().redrawNow();
    }
  }

  /*************************************** getColumnsAxis ****************************************/
  public TableAxis getColumnsAxis()
  {
    // return columns (horizontal) axis for cell widths & x-coordinates
    return m_columnsAxis;
  }

  /***************************************** getRowsAxis *****************************************/
  public TableAxis getRowsAxis()
  {
    // return rows (vertical) axis for cell heights & y-coordinates
    return m_rowsAxis;
  }

  /******************************************* resize ********************************************/
  @Override
  public void resize( double width, double height )
  {
    // resize the canvas and overlay
    getOverlay().setWidth( width );
    getOverlay().setHeight( height );
    setWidth( width );
    setHeight( height );
  }

}
