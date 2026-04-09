<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listDevices } from "@/api/devices";
import {
    approveReservation,
    cancelReservation,
    createReservation,
    getReservationDetail,
    listReservations
} from "@/api/reservations";
import { useAuthStore } from "@/store/auth";
import type { DeviceItem } from "@/types/device";
import type {
    ApproveReservationPayload,
    CreateReservationPayload,
    ReservationItem,
    ReservationListQuery,
    ReservationStatus
} from "@/types/reservation";

const authStore = useAuthStore();
const loading = ref(false);
const createDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const createFormRef = ref();
const detailLoading = ref(false);

const devices = ref<DeviceItem[]>([]);
const detail = ref<ReservationItem | null>(null);

const filters = reactive<ReservationListQuery>({
    status: undefined,
    pageNum: 1,
    pageSize: 10
});

const page = reactive({
    list: [] as ReservationItem[],
    total: 0
});

const createForm = reactive({
    deviceId: undefined as number | undefined,
    startTime: "",
    endTime: "",
    purpose: ""
});

const dateTimeValueFormat = "YYYY-MM-DD HH:mm";
const currentRole = computed(
    () => authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? "STUDENT"
);
const canCreate = computed(() => currentRole.value === "STUDENT");
const isTeacher = computed(() => currentRole.value === "TEACHER");
const isAdmin = computed(() => currentRole.value === "SUPER_ADMIN" || currentRole.value === "ADMIN");
const statusOptions: ReservationStatus[] = [
    "PENDING",
    "APPROVED",
    "PICKUP_PENDING",
    "REJECTED",
    "EXPIRED",
    "CANCELLED"
];

function toApiDateTime(value: string) {
    return value.length === 16 ? `${value}:00` : value;
}

const validateTimeRange = (_rule: unknown, _value: string, callback: (error?: Error) => void) => {
    if (createForm.startTime && createForm.endTime && createForm.endTime <= createForm.startTime) {
        callback(new Error("结束时间必须晚于开始时间"));
        return;
    }
    callback();
};

const createRules = {
    deviceId: [{ required: true, message: "请选择设备", trigger: "change" }],
    startTime: [
        { required: true, message: "请选择开始时间", trigger: "change" },
        { validator: validateTimeRange, trigger: "change" }
    ],
    endTime: [
        { required: true, message: "请选择结束时间", trigger: "change" },
        { validator: validateTimeRange, trigger: "change" }
    ],
    purpose: [{ required: true, message: "请输入用途说明", trigger: "blur" }]
};

function reservationStatusLabel(status: ReservationStatus) {
    switch (status) {
        case "PENDING":
            return "待教师审核";
        case "APPROVED":
            return "待管理员审核";
        case "PICKUP_PENDING":
            return "待领取";
        case "REJECTED":
            return "已驳回";
        case "EXPIRED":
            return "已过期";
        case "CANCELLED":
            return "已取消";
        default:
            return status;
    }
}

function disabledStartDate(date: Date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date.getTime() < today.getTime();
}

function disabledEndDate(date: Date) {
    if (!createForm.startTime) {
        return disabledStartDate(date);
    }

    const startDate = new Date(createForm.startTime.replace(/-/g, "/"));
    startDate.setHours(0, 0, 0, 0);
    return date.getTime() < startDate.getTime();
}

function handleStartTimeChange(value: string) {
    if (value && createForm.endTime && createForm.endTime <= value) {
        createForm.endTime = "";
    }
}

function canApprove(row: ReservationItem) {
    return (isTeacher.value && row.status === "PENDING") || (isAdmin.value && row.status === "APPROVED");
}

function canReject(row: ReservationItem) {
    return (
        (isTeacher.value && row.status === "PENDING") ||
        (isAdmin.value && (row.status === "PENDING" || row.status === "APPROVED"))
    );
}

function canCancel(row: ReservationItem) {
    return currentRole.value === "STUDENT" && (row.status === "PENDING" || row.status === "APPROVED");
}

function approveButtonText(row: ReservationItem) {
    if (isAdmin.value && row.status === "APPROVED") {
        return "终审通过";
    }
    return "教师通过";
}

async function loadReservations() {
    loading.value = true;

    try {
        const data = await listReservations(filters);
        page.list = data.list;
        page.total = data.total;
    } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "加载预约记录失败");
    } finally {
        loading.value = false;
    }
}

async function loadDevicesForCreate() {
    try {
        const data = await listDevices({
            pageNum: 1,
            pageSize: 100
        });
        devices.value = data.list.filter((item) => item.status === "AVAILABLE");
    } catch {
        devices.value = [];
    }
}

function handleSearch() {
    filters.pageNum = 1;
    void loadReservations();
}

function handleReset() {
    filters.status = undefined;
    filters.pageNum = 1;
    void loadReservations();
}

function resetCreateForm() {
    createForm.deviceId = undefined;
    createForm.startTime = "";
    createForm.endTime = "";
    createForm.purpose = "";
}

async function submitCreate() {
    await createFormRef.value?.validate();

    try {
        await createReservation({
            deviceId: createForm.deviceId!,
            startTime: toApiDateTime(createForm.startTime),
            endTime: toApiDateTime(createForm.endTime),
            purpose: createForm.purpose
        } satisfies CreateReservationPayload);
        ElMessage.success("预约申请已提交");
        createDialogVisible.value = false;
        resetCreateForm();
        await loadReservations();
    } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "提交失败");
    }
}

async function openDetail(reservationId: number) {
    detailDialogVisible.value = true;
    detailLoading.value = true;

    try {
        detail.value = await getReservationDetail(reservationId);
    } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "加载详情失败");
    } finally {
        detailLoading.value = false;
    }
}

async function handleReview(row: ReservationItem, action: "APPROVE" | "REJECT") {
    let comment = "";

    if (action === "REJECT") {
        comment = await ElMessageBox.prompt("请输入驳回原因", "驳回预约", {
            confirmButtonText: "确认驳回",
            cancelButtonText: "取消",
            inputPattern: /\S+/,
            inputErrorMessage: "请输入原因"
        }).then((result) => result.value);
    }

    try {
        await approveReservation(row.reservationId, {
            action,
            comment
        } satisfies ApproveReservationPayload);

        if (action === "APPROVE") {
            ElMessage.success(isAdmin.value && row.status === "APPROVED" ? "预约已完成终审" : "预约已通过教师审核");
        } else {
            ElMessage.success("预约已驳回");
        }
        await loadReservations();
    } catch (error) {
        if (error === "cancel") {
            return;
        }
        ElMessage.error(error instanceof Error ? error.message : "审核失败");
    }
}

async function handleCancel(row: ReservationItem) {
    try {
        await ElMessageBox.confirm("确认取消这条预约申请吗？", "取消预约", {
            confirmButtonText: "确认取消",
            cancelButtonText: "返回",
            type: "warning"
        });
        await cancelReservation(row.reservationId);
        ElMessage.success("预约已取消");
        await loadReservations();
    } catch (error) {
        if (error === "cancel") {
            return;
        }
        ElMessage.error(error instanceof Error ? error.message : "取消失败");
    }
}

onMounted(() => {
    void Promise.all([loadReservations(), loadDevicesForCreate()]);
});
</script>

<template>
    <div class="reservation-page">
        <section class="hero-card">
            <span class="eyebrow">预约模块</span>
            <h2>预约申请与两级审核流程</h2>
            <p>学生提交后需要先教师审核，教师通过后再进入管理员终审，只有两级都通过才可以领取。</p>
        </section>

        <section class="toolbar-card">
            <div class="filter-grid">
                <ElSelect v-model="filters.status" placeholder="预约状态" clearable>
                    <ElOption
                        v-for="status in statusOptions"
                        :key="status"
                        :label="reservationStatusLabel(status)"
                        :value="status"
                    />
                </ElSelect>
            </div>

            <div class="toolbar-actions">
                <ElButton @click="handleReset">重置</ElButton>
                <ElButton type="primary" @click="handleSearch">查询</ElButton>
                <ElButton v-if="canCreate" type="success" @click="createDialogVisible = true">发起预约</ElButton>
            </div>
        </section>

        <section class="table-card">
            <div class="table-scroll">
                <ElTable :data="page.list" v-loading="loading" width="100%">
                    <ElTableColumn prop="deviceName" label="设备" min-width="180" />
                    <ElTableColumn prop="applicantName" label="申请人" min-width="140" />
                    <ElTableColumn prop="startTime" label="开始时间" min-width="160" />
                    <ElTableColumn prop="endTime" label="结束时间" min-width="160" />
                    <ElTableColumn prop="status" label="状态" min-width="140">
                        <template #default="{ row }">
                            <ElTag
                                :type="
                                    row.status === 'PENDING'
                                        ? 'warning'
                                        : row.status === 'APPROVED'
                                          ? 'info'
                                          : row.status === 'PICKUP_PENDING'
                                            ? 'success'
                                            : 'info'
                                "
                            >
                                {{ reservationStatusLabel(row.status) }}
                            </ElTag>
                        </template>
                    </ElTableColumn>
                    <ElTableColumn prop="purpose" label="用途" min-width="220" />
                    <ElTableColumn label="操作" min-width="320" fixed="right">
                        <template #default="{ row }">
                            <div class="action-row">
                                <ElButton link type="primary" @click="openDetail(row.reservationId)">详情</ElButton>
                                <ElButton
                                    v-if="canApprove(row)"
                                    link
                                    type="success"
                                    @click="handleReview(row, 'APPROVE')"
                                >
                                    {{ approveButtonText(row) }}
                                </ElButton>
                                <ElButton v-if="canReject(row)" link type="danger" @click="handleReview(row, 'REJECT')">
                                    驳回
                                </ElButton>
                                <ElButton v-if="canCancel(row)" link type="warning" @click="handleCancel(row)">
                                    取消
                                </ElButton>
                            </div>
                        </template>
                    </ElTableColumn>
                </ElTable>
            </div>

            <div class="pagination-wrap">
                <ElPagination
                    v-model:current-page="filters.pageNum"
                    v-model:page-size="filters.pageSize"
                    background
                    layout="total, prev, pager, next"
                    :total="page.total"
                    @current-change="loadReservations"
                />
            </div>
        </section>

        <ElDialog v-model="createDialogVisible" title="发起预约" width="520px" @closed="resetCreateForm">
            <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
                <ElFormItem label="设备" prop="deviceId">
                    <ElSelect v-model="createForm.deviceId" placeholder="请选择可预约设备">
                        <ElOption
                            v-for="device in devices"
                            :key="device.deviceId"
                            :label="`${device.deviceName} / ${device.deviceCode}`"
                            :value="device.deviceId"
                        />
                    </ElSelect>
                </ElFormItem>
                <ElFormItem label="开始时间" prop="startTime">
                    <ElDatePicker
                        v-model="createForm.startTime"
                        type="datetime"
                        placeholder="请选择开始时间"
                        :disabled-date="disabledStartDate"
                        :value-format="dateTimeValueFormat"
                        format="YYYY-MM-DD HH:mm"
                        style="width: 100%"
                        @change="handleStartTimeChange"
                    />
                </ElFormItem>
                <ElFormItem label="结束时间" prop="endTime">
                    <ElDatePicker
                        v-model="createForm.endTime"
                        type="datetime"
                        placeholder="请选择结束时间"
                        :disabled-date="disabledEndDate"
                        :value-format="dateTimeValueFormat"
                        format="YYYY-MM-DD HH:mm"
                        style="width: 100%"
                    />
                </ElFormItem>
                <ElFormItem label="用途说明" prop="purpose">
                    <ElInput v-model="createForm.purpose" type="textarea" :rows="3" placeholder="请填写预约用途" />
                </ElFormItem>
            </ElForm>
            <template #footer>
                <ElButton @click="createDialogVisible = false">取消</ElButton>
                <ElButton type="primary" @click="submitCreate">提交</ElButton>
            </template>
        </ElDialog>

        <ElDialog v-model="detailDialogVisible" title="预约详情" width="560px">
            <ElSkeleton v-if="detailLoading" :rows="7" animated />
            <div v-else-if="detail" class="detail-grid">
                <article>
                    <strong>设备</strong><span>{{ detail.deviceName }}</span>
                </article>
                <article>
                    <strong>申请人</strong><span>{{ detail.applicantName }}</span>
                </article>
                <article>
                    <strong>开始时间</strong><span>{{ detail.startTime }}</span>
                </article>
                <article>
                    <strong>结束时间</strong><span>{{ detail.endTime }}</span>
                </article>
                <article>
                    <strong>状态</strong><span>{{ reservationStatusLabel(detail.status) }}</span>
                </article>
                <article>
                    <strong>创建时间</strong><span>{{ detail.createdAt || "--" }}</span>
                </article>
                <article class="full-width">
                    <strong>用途说明</strong><span>{{ detail.purpose }}</span>
                </article>
                <article class="full-width">
                    <strong>审核备注</strong><span>{{ detail.reviewComment || "--" }}</span>
                </article>
            </div>
        </ElDialog>
    </div>
</template>

<style scoped>
.reservation-page {
    display: grid;
    gap: 20px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: hidden;
}

.hero-card,
.toolbar-card,
.table-card {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    padding: 24px;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card h2 {
    margin: 10px 0 12px;
    font-size: 30px;
}

.hero-card p {
    margin: 0;
    line-height: 1.7;
    color: #475569;
}

.eyebrow {
    display: inline-flex;
    width: fit-content;
    padding: 6px 10px;
    border-radius: 999px;
    color: var(--theme-primary);
    background: var(--theme-primary-soft);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.toolbar-card {
    display: grid;
    gap: 16px;
}

.filter-grid {
    display: grid;
    grid-template-columns: 280px;
    gap: 14px;
}

.toolbar-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
    flex-wrap: wrap;
}

.action-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.table-scroll {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: auto;
}

.table-scroll :deep(.el-table) {
    min-width: 1200px;
}

.pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
}

.detail-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.detail-grid article {
    display: grid;
    gap: 6px;
    padding: 16px;
    border-radius: 18px;
    background: #f8fafc;
}

.detail-grid span {
    color: #475569;
}

.full-width {
    grid-column: 1 / -1;
}

@media (max-width: 960px) {
    .filter-grid,
    .detail-grid {
        grid-template-columns: 1fr;
    }

    .hero-card,
    .toolbar-card,
    .table-card {
        padding: 18px;
    }

    .toolbar-actions {
        justify-content: flex-start;
    }

    .pagination-wrap {
        justify-content: flex-start;
    }
}
</style>
