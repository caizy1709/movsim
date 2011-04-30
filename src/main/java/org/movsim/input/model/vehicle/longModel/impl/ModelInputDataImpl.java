/**
 * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber,
 *                             Ralph Germ, Martin Budden
 *                             <info@movsim.org>
 * ----------------------------------------------------------------------
 * 
 *  This file is part of 
 *  
 *  MovSim - the multi-model open-source vehicular-traffic simulator 
 *
 *  MovSim is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MovSim is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MovSim.  If not, see <http://www.gnu.org/licenses/> or
 *  <http://www.movsim.org>.
 *  
 * ----------------------------------------------------------------------
 */
package org.movsim.input.model.vehicle.longModel.impl;

import org.movsim.input.model.vehicle.longModel.ModelInputData;

// TODO: Auto-generated Javadoc
/**
 * The Class ModelInputDataImpl.
 */
public abstract class ModelInputDataImpl implements ModelInputData {

    /** The model name. */
    private final String modelName;

    /**
     * Instantiates a new model input data impl.
     * 
     * @param modelName
     *            the model name
     */
    public ModelInputDataImpl(String modelName) {
        this.modelName = modelName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.movsim.input.model.vehicle.longModel.ModelInputData#getModelName()
     */
    @Override
    public String getModelName() {
        return modelName;
    }

}
