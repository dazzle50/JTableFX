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

package rjc.table.undo.commands;

import java.util.Arrays;

import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.action.Sort.SortType;
import rjc.table.view.action.SortList;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/***************** UndoCommand for sorting table-view by specified column or row *****************/
/*************************************************************************************************/

public class CommandSortView implements IUndoCommand
{
  private TableView m_view;     // table view
  private TableAxis m_axis;     // axis being reordered
  private int       m_index;    // view index being sorted
  private int[]     m_oldOrder; // data-indexes order before sort
  private int[]     m_newOrder; // data-indexes order after sort
  private String    m_text;     // text describing command

  /**************************************** constructor ******************************************/
  public CommandSortView( TableView view, TableAxis axis, int viewIndex, SortType type )
  {
    // create SortList from specified column or row in specified view
    SortList list = new SortList();
    boolean isColumn = axis == view.getColumnsAxis();
    if ( isColumn )
    {
      m_axis = view.getRowsAxis();
      int dataColumn = axis.getDataIndex( viewIndex );
      for ( int viewRow = 0; viewRow < view.getData().getRowCount(); viewRow++ )
      {
        int dataRow = m_axis.getDataIndex( viewRow );
        if ( m_axis.getNominalSize( dataRow ) > 0 ) // only include visible rows
          list.append( dataRow, view.getData().getValue( dataColumn, dataRow ) );
      }
    }
    else
    {
      m_axis = view.getColumnsAxis();
      int dataRow = axis.getDataIndex( viewIndex );
      for ( int viewColumn = 0; viewColumn < view.getData().getColumnCount(); viewColumn++ )
      {
        int dataColumn = m_axis.getDataIndex( viewColumn );
        if ( m_axis.getNominalSize( dataColumn ) > 0 ) // only include visible columns
          list.append( dataColumn, view.getData().getValue( dataColumn, dataRow ) );
      }
    }

    // store old order, sort list and store new order
    m_oldOrder = list.getIndexes();
    list.sort( type );
    m_newOrder = list.getIndexes();

    // if no change in order, do not create command
    if ( Arrays.equals( m_oldOrder, m_newOrder ) )
      return;

    m_view = view;
    m_index = viewIndex;
    redo();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // apply the sorted index mapping
    m_axis.setMapping( m_newOrder );
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // apply the before-sort index mapping
    m_axis.setMapping( m_oldOrder );
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // construct command description
    if ( m_text == null )
    {
      m_text = "Sorted ";
      if ( m_axis == m_view.getRowsAxis() )
      {
        int dataColumn = m_view.getColumnsAxis().getDataIndex( m_index );
        m_text += "column " + m_view.getData().getValue( dataColumn, TableAxis.HEADER );
      }
      else
      {
        int dataRow = m_view.getRowsAxis().getDataIndex( m_index );
        m_text += "row " + m_view.getData().getValue( TableAxis.HEADER, dataRow );
      }

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is valid only if change in order (indicated by m_view not null)
    return m_view != null;
  }

}
