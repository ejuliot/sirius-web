/*******************************************************************************
 * Copyright (c) 2024 Obeo.
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
package org.eclipse.sirius.web.application.controllers.trees;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.trees.dto.TreeEventInput;
import org.eclipse.sirius.components.collaborative.trees.dto.TreeRefreshedEventPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IIdentityService;
import org.eclipse.sirius.components.core.api.IInput;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.components.graphql.tests.ExecuteEditingContextFunctionInput;
import org.eclipse.sirius.components.graphql.tests.ExecuteEditingContextFunctionRunner;
import org.eclipse.sirius.components.graphql.tests.ExecuteEditingContextFunctionSuccessPayload;
import org.eclipse.sirius.components.trees.Tree;
import org.eclipse.sirius.components.trees.tests.graphql.ExpandAllTreePathQueryRunner;
import org.eclipse.sirius.components.trees.tests.graphql.TreeEventSubscriptionRunner;
import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.web.AbstractIntegrationTests;
import org.eclipse.sirius.web.application.views.explorer.services.ExplorerDescriptionProvider;
import org.eclipse.sirius.web.data.StudioIdentifiers;
import org.eclipse.sirius.web.services.PapayaViewInjector;
import org.eclipse.sirius.web.services.TaskViewInjector;
import org.eclipse.sirius.web.tests.services.api.IGivenInitialServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import graphql.execution.DataFetcherResult;
import reactor.test.StepVerifier;

/**
 * Integration tests of the computation of the expand all tree path in the explorer.
 *
 * @author sbegaudeau
 */
@Transactional
@SuppressWarnings("checkstyle:MultipleStringLiterals")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExplorerExpandAllControllerTests extends AbstractIntegrationTests {

    @Autowired
    private IGivenInitialServerState givenInitialServerState;

    @Autowired
    private TreeEventSubscriptionRunner treeEventSubscriptionRunner;

    @Autowired
    private ExecuteEditingContextFunctionRunner executeEditingContextFunctionRunner;

    @Autowired
    private PapayaViewInjector papayaViewInjector;

    @Autowired
    private TaskViewInjector taskViewInjector;

    @Autowired
    private ExpandAllTreePathQueryRunner expandAllTreePathQueryRunner;

    @Autowired
    private IIdentityService identityService;

    @BeforeEach
    public void beforeEach() {
        this.givenInitialServerState.initialize();
    }

    @Test
    @DisplayName("Given a studio, when we ask for the tree path to expand a papaya view object, then its path in the explorer is returned")
    @Sql(scripts = {"/scripts/studio.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/scripts/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    public void givenStudioWhenWeAskForTheTreePathOfPapayaViewObjectThenItsPathInTheExplorerIsReturned() {
        this.givenStudioWhenWeAskForTheTreePathOfAnObjectThenItsPathInTheExplorerIsReturned(this.papayaViewInjector);
    }

    @Test
    @DisplayName("Given a studio, when we ask for the tree path to expand a task view object, then its path in the explorer is returned")
    @Sql(scripts = {"/scripts/studio.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/scripts/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    public void givenStudioWhenWeAskForTheTreePathOfTaskViewObjectThenItsPathInTheExplorerIsReturned() {
        this.givenStudioWhenWeAskForTheTreePathOfAnObjectThenItsPathInTheExplorerIsReturned(this.taskViewInjector);
    }

    public void givenStudioWhenWeAskForTheTreePathOfAnObjectThenItsPathInTheExplorerIsReturned(BiFunction<IEditingContext, IInput, IPayload> objectInjector) {
        var input = new TreeEventInput(UUID.randomUUID(), StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(), ExplorerDescriptionProvider.PREFIX, List.of(), List.of());
        var flux = this.treeEventSubscriptionRunner.run(input);

        var treeId = new AtomicReference<String>();
        var objectId = new AtomicReference<String>();

        Consumer<Object> initialTreeContentConsumer = this.getTreeSubscriptionConsumer(tree -> {
            assertThat(tree).isNotNull();
            assertThat(tree.getChildren()).hasSize(1);
            assertThat(tree.getChildren()).allSatisfy(treeItem -> assertThat(treeItem.getChildren()).isEmpty());

            treeId.set(tree.getId());
        });

        Runnable createView = () -> {
            var createViewInput = new ExecuteEditingContextFunctionInput(UUID.randomUUID(), StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(), objectInjector);
            this.executeEditingContextFunctionRunner.execute(createViewInput).block();

            BiFunction<IEditingContext, IInput, IPayload> getObjectIdFunction = (editingContext, executeEditingContextFunctionInput) -> {
                var id = Optional.of(editingContext)
                        .filter(IEMFEditingContext.class::isInstance)
                        .map(IEMFEditingContext.class::cast)
                        .flatMap(emfEditingContext -> {
                            var iterator = emfEditingContext.getDomain().getResourceSet().getAllContents();
                            var stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
                            return stream.filter(RepresentationDescription.class::isInstance)
                                    .map(RepresentationDescription.class::cast)
                                    .findFirst()
                                    .map(EObject::eResource);
                        })
                        .filter(Resource.class::isInstance)
                        .map(Resource.class::cast)
                        .map(resource -> resource.getURI().path().substring(1))
                        .orElse("");
                return new ExecuteEditingContextFunctionSuccessPayload(executeEditingContextFunctionInput.id(), id);
            };

            var getObjectIdInput = new ExecuteEditingContextFunctionInput(UUID.randomUUID(), StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(), getObjectIdFunction, new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(), createViewInput));
            var payload = this.executeEditingContextFunctionRunner.execute(getObjectIdInput).block();

            assertThat(payload).isInstanceOf(ExecuteEditingContextFunctionSuccessPayload.class);
            ExecuteEditingContextFunctionSuccessPayload successPayload = (ExecuteEditingContextFunctionSuccessPayload) payload;
            objectId.set(successPayload.result().toString());
        };

        Consumer<Object> updatedTreeContentConsumer = object -> this.getTreeSubscriptionConsumer(tree -> {
            assertThat(tree).isNotNull();
            assertThat(tree.getChildren()).hasSize(2);
            assertThat(tree.getChildren()).allSatisfy(treeItem -> assertThat(treeItem.getChildren()).isEmpty());
        });

        var treeItemIds = new AtomicReference<List<String>>();

        Runnable getTreePath = () -> {
            Map<String, Object> variables = Map.of(
                    "editingContextId", StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(),
                    "treeId", treeId.get(),
                    "treeItemId", objectId.get()
            );
            var result = this.expandAllTreePathQueryRunner.run(variables);
            List<String> treeItemIdsToExpand = JsonPath.read(result, "$.data.viewer.editingContext.expandAllTreePath.treeItemIdsToExpand");
            assertThat(treeItemIdsToExpand).isNotEmpty();

            treeItemIds.set(treeItemIdsToExpand);
        };

        StepVerifier.create(flux)
                .consumeNextWith(initialTreeContentConsumer)
                .then(createView)
                .consumeNextWith(updatedTreeContentConsumer)
                .then(getTreePath)
                .thenCancel()
                .verify(Duration.ofSeconds(10));

        var expandedTreeInput = new TreeEventInput(UUID.randomUUID(), StudioIdentifiers.EMPTY_STUDIO_PROJECT.toString(), ExplorerDescriptionProvider.PREFIX, treeItemIds.get(), List.of());
        var expandedTreeFlux = this.treeEventSubscriptionRunner.run(expandedTreeInput);

        Consumer<Object> initialExpandedTreeContentConsumer = this.getTreeSubscriptionConsumer(tree -> {
            assertThat(tree).isNotNull();
            assertThat(tree.getChildren()).hasSize(2);
            assertThat(tree.getChildren()).anySatisfy(treeItem -> assertThat(treeItem.getChildren()).isNotEmpty());

            treeId.set(tree.getId());
        });

        StepVerifier.create(expandedTreeFlux)
                .consumeNextWith(initialExpandedTreeContentConsumer)
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }

    private Consumer<Object> getTreeSubscriptionConsumer(Consumer<Tree> treeConsumer) {
        return object -> Optional.of(object)
                .filter(DataFetcherResult.class::isInstance)
                .map(DataFetcherResult.class::cast)
                .map(DataFetcherResult::getData)
                .filter(TreeRefreshedEventPayload.class::isInstance)
                .map(TreeRefreshedEventPayload.class::cast)
                .map(TreeRefreshedEventPayload::tree)
                .ifPresentOrElse(treeConsumer, () -> fail("Missing tree"));
    }
}
