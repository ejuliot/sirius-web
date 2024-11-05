/*******************************************************************************
 * Copyright (c) 2021, 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/

import { ComponentExtensionPoint } from '@eclipse-sirius/sirius-components-core';
import { EditProjectNavbarMenuContainerProps, EditProjectNavbarMenuEntryProps } from './EditProjectNavbar.types';

const FallbackEditProjectNavbarMenuContainer = ({ children }: EditProjectNavbarMenuContainerProps) => {
  return <div>{children}</div>;
};

export const editProjectNavbarMenuContainerExtensionPoint: ComponentExtensionPoint<EditProjectNavbarMenuContainerProps> =
  {
    identifier: 'editProjectNavbarMenu#container',
    FallbackComponent: FallbackEditProjectNavbarMenuContainer,
  };

export const editProjectNavbarMenuEntryExtensionPoint: ComponentExtensionPoint<EditProjectNavbarMenuEntryProps> = {
  identifier: 'editProjectNavbarMenu#entry',
  FallbackComponent: () => null,
};