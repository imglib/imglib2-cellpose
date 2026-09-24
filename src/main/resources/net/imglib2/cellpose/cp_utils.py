###
# #%L
# Running Cellpose 3 and 4 from Java with Appose, using ImgLib2 data structure.
# %%
# Copyright (C) 2026 Appose developpers
# %%
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# 3. Neither the name of the ImgLib2 nor the names of its contributors
#    may be used to endorse or promote products derived from this software without
#    specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
# IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
# INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
# OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
# OF THE POSSIBILITY OF SUCH DAMAGE.
# #L%
###

# These imports are required for Appose calls to work on Windows platforms.
import numpy as np
import torch
from cellpose import models, io
from typing import TYPE_CHECKING


def get_torch_device(use_gpu: bool) -> tuple[bool, torch.device]:
    """Check torch device availability and returns a tupple (use_gpu: bool, device: torch.device) using the best available backend: CUDA > MPS > CPU."""
    if not use_gpu:
        return False, torch.device("cpu")

    if torch.cuda.is_available():
        return True, torch.device("cuda")

    if torch.backends.mps.is_available():
        return True, torch.device("mps")

    return False, torch.device("cpu")


# %%
def unique_labels(masks):
    """Ensure that labels are unique in each slice"""
    masks = np.asarray(masks)

    # 2D image: do nothing. Labels are already unique in the plane
    if masks.ndim <= 2:
        return masks

    nz = masks.shape[0]
    max_per_z = masks.reshape(nz, -1).max(axis=1).astype(masks.dtype, copy=False)
    ## Calculate the offsets for each slice
    offsets = np.concatenate(([0], np.cumsum(max_per_z, dtype=masks.dtype)[:-1]))
    offsets = offsets.reshape((nz,) + (1,) * (masks.ndim - 1))
    ## Offset only positive pixels (labels)
    masks = np.where(masks > 0, masks + offsets, masks)
    return masks


def shuffle_labels(masks):
    """Randomize the position of the labels so close value are not necessarily close"""
    masks = np.asarray(masks)
    labels = np.unique(masks)
    if len(labels) <= 1:
        return masks
    labels = labels[labels != 0]  ## remove 0

    shuffled_labels = np.random.permutation(labels)
    max_label = labels.max()
    indexes = np.zeros(max_label + 1, dtype=masks.dtype)
    indexes[labels] = shuffled_labels
    rand_masks = indexes[masks]

    return rand_masks
