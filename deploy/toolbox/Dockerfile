FROM docker:27-cli

ARG TARGETARCH=amd64
ARG KIND_VERSION=v0.24.0
ARG KUBECTL_VERSION=v1.31.0

RUN apk add --no-cache bash ca-certificates curl git openssh-client

RUN case "${TARGETARCH}" in \
      amd64) KIND_ARCH=amd64; KUBECTL_ARCH=amd64 ;; \
      arm64) KIND_ARCH=arm64; KUBECTL_ARCH=arm64 ;; \
      *) echo "Unsupported architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-linux-${KIND_ARCH}" -o /usr/local/bin/kind \
    && chmod +x /usr/local/bin/kind \
    && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${KUBECTL_ARCH}/kubectl" -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl

COPY scripts/ /usr/local/bin/
RUN chmod +x /usr/local/bin/pep-kind-*

WORKDIR /workspace

ENV KUBECONFIG=/kube/config
ENV KIND_CLUSTER_NAME=pep-local

CMD ["sleep", "infinity"]
