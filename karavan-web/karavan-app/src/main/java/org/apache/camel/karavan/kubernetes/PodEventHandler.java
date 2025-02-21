package org.apache.camel.karavan.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.apache.camel.karavan.infinispan.InfinispanService;
import org.apache.camel.karavan.infinispan.model.ContainerStatus;
import org.jboss.logging.Logger;

import java.util.List;

import static org.apache.camel.karavan.service.CodeService.DEFAULT_CONTAINER_RESOURCES;
import static org.apache.camel.karavan.shared.Constants.LABEL_PROJECT_ID;
import static org.apache.camel.karavan.shared.EventType.CONTAINER_STATUS;

public class PodEventHandler implements ResourceEventHandler<Pod> {

    private static final Logger LOGGER = Logger.getLogger(PodEventHandler.class.getName());
    private final InfinispanService infinispanService;
    private final KubernetesService kubernetesService;
    private final EventBus eventBus;

    public PodEventHandler(InfinispanService infinispanService, KubernetesService kubernetesService, EventBus eventBus) {
        this.infinispanService = infinispanService;
        this.kubernetesService = kubernetesService;
        this.eventBus = eventBus;
    }

    @Override
    public void onAdd(Pod pod) {
        try {
            LOGGER.info("onAdd " + pod.getMetadata().getName());
            ContainerStatus ps = getPodStatus(pod);
            if (ps != null) {
                eventBus.send(CONTAINER_STATUS, JsonObject.mapFrom(ps));
            }
        } catch (Exception e){
            LOGGER.error(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void onUpdate(Pod oldPod, Pod newPod) {
        try {
            LOGGER.info("onUpdate " + newPod.getMetadata().getName());
            ContainerStatus ps = getPodStatus(newPod);
            if (ps != null) {
                eventBus.send(CONTAINER_STATUS, JsonObject.mapFrom(ps));
            }
        } catch (Exception e){
            LOGGER.error(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
        try {
            LOGGER.info("onDelete " + pod.getMetadata().getName());
            String deployment = pod.getMetadata().getLabels().get("app");
            String projectId = deployment != null ? deployment : pod.getMetadata().getLabels().get(LABEL_PROJECT_ID);
            infinispanService.deleteContainerStatus(projectId, kubernetesService.environment, pod.getMetadata().getName());
        } catch (Exception e){
            LOGGER.error(e.getMessage(), e.getCause());
        }
    }


    public ContainerStatus getPodStatus(Pod pod) {
        String deployment = pod.getMetadata().getLabels().get("app");
        String projectId = deployment != null ? deployment : pod.getMetadata().getLabels().get(LABEL_PROJECT_ID);
        try {
            boolean ready = pod.getStatus().getConditions().stream().anyMatch(c -> c.getType().equals("Ready"));
            String creationTimestamp = pod.getMetadata().getCreationTimestamp();

            ResourceRequirements defaultRR = kubernetesService.getResourceRequirements(DEFAULT_CONTAINER_RESOURCES);
            ResourceRequirements resourceRequirements = pod.getSpec().getContainers().stream().findFirst()
                    .orElse(new ContainerBuilder().withResources(defaultRR).build()).getResources();

            String requestMemory = resourceRequirements.getRequests().getOrDefault("memory", new Quantity()).toString();
            String requestCpu = resourceRequirements.getRequests().getOrDefault("cpu", new Quantity()).toString();
            String limitMemory = resourceRequirements.getLimits().getOrDefault("memory", new Quantity()).toString();
            String limitCpu = resourceRequirements.getLimits().getOrDefault("cpu", new Quantity()).toString();
            ContainerStatus status = new ContainerStatus(
                    pod.getMetadata().getName(),
                    List.of(ContainerStatus.Command.delete),
                    projectId,
                    kubernetesService.environment,
                    pod.getMetadata().getName().equals(projectId) ? ContainerStatus.ContainerType.devmode : ContainerStatus.ContainerType.project,
                    requestMemory + " / " + limitMemory,
                    requestCpu + " / " + limitCpu,
                    creationTimestamp);

            if (ready) {
                status.setState(ContainerStatus.State.running.name());
            } else {
                status.setState(ContainerStatus.State.created.name());
            }
            return status;
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex.getCause());
            return null;
        }
    }
}