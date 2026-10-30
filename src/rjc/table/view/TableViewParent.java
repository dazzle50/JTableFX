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

package rjc.table.view;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/*************************************************************************************************/
/****************************** JavaFX parent node for table views *******************************/
/*************************************************************************************************/

/**
 * A custom JavaFX {@link Region} that serves as the container for table view components.
 * <p>
 * This class acts as the root parent node for a table view implementation. It manages the
 * allocated size through the standard double-valued Region properties. Its layout bounds remain
 * (0, 0, width, height) even when a child extends outside the table. It fills available space, allowing
 * scrollbars to appear at the edges of the allocated area when content exceeds the visible region.
 * <p>
 * The node is resizable and computes extremely large preferred dimensions by default.
 * Applications can set finite preferred dimensions using the standard Region API.
 * Integer drawing extents are available through {@link #getPixelWidth()} and {@link #getPixelHeight()};
 * these truncate layout coordinates and are not physical monitor-pixel measurements.
 * <p>
 * This class also provides convenience methods for adding, removing, and checking
 * membership of child nodes that are part of the table's visual representation.
 */
public class TableViewParent extends Region
{
  /************************************** computeMinHeight ***************************************/
  /**
   * Returns the minimum height this node is willing to be resized to.
   *
   * @param width the width constraint (ignored in this implementation)
   * @return 0.0 — no minimum height constraint
   */
  @Override
  protected double computeMinHeight( double width )
  {
    return 0.0;
  }

  /*************************************** computeMinWidth ***************************************/
  /**
   * Returns the minimum width this node is willing to be resized to.
   *
   * @param height the height constraint (ignored in this implementation)
   * @return 0.0 — no minimum width constraint
   */
  @Override
  protected double computeMinWidth( double height )
  {
    return 0.0;
  }

  /************************************** computePrefHeight **************************************/
  /**
   * Returns the preferred height of this node for layout purposes.
   * <p>
   * A very large value is returned so that the parent layout allocates the maximum
   * available vertical space, allowing scrollbars to appear at the edge of the area.
   *
   * @param width the width constraint (ignored in this implementation)
   * @return {@code Integer.MAX_VALUE} to request maximum available height
   */
  @Override
  protected double computePrefHeight( double width )
  {
    return Integer.MAX_VALUE;
  }

  /*************************************** computePrefWidth **************************************/
  /**
   * Returns the preferred width of this node for layout purposes.
   * <p>
   * A very large value is returned so that the parent layout allocates the maximum
   * available horizontal space, allowing scrollbars to appear at the edge of the area.
   *
   * @param height the height constraint (ignored in this implementation)
   * @return {@code Integer.MAX_VALUE} to request maximum available width
   */
  @Override
  protected double computePrefWidth( double height )
  {
    return Integer.MAX_VALUE;
  }

  /**************************************** getPixelWidth ****************************************/
  /**
   * Returns the current integer drawing width by truncating the allocated layout width.
   *
   * @return current integer width
   */
  public int getPixelWidth()
  {
    return (int) getWidth();
  }

  /**************************************** getPixelHeight ***************************************/
  /**
   * Returns the current integer drawing height by truncating the allocated layout height.
   *
   * @return current integer height
   */
  public int getPixelHeight()
  {
    return (int) getHeight();
  }

  /********************************************* add *********************************************/
  /**
   * Adds a child node to be displayed as part of the table.
   *
   * @param node the node to add to the table's children list
   */
  public void add( Node node )
  {
    getChildren().add( node );
  }

  /******************************************* remove ********************************************/
  /**
   * Removes a child node from the table's displayed content.
   *
   * @param node the node to remove from the table's children list
   */
  public void remove( Node node )
  {
    getChildren().remove( node );
  }

  /****************************************** contains *******************************************/
  /**
   * Checks whether the specified node is currently a child of this table parent.
   *
   * @param node the node to test for membership
   * @return {@code true} if the node is in the children list, otherwise {@code false}
   */
  public boolean contains( Node node )
  {
    return getChildren().contains( node );
  }
}