<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdmin,
  createStudent,
  createTeacher,
  deleteStudent,
  getUserDetail,
  listUsers,
  resetPassword,
  updateUserStatus
} from '@/api/users'
import { useAuthStore } from '@/store/auth'
import type {
  CreateAdminPayload,
  CreateStudentPayload,
  CreateTeacherPayload,
  ResetPasswordPayload,
  UserListItem,
  UserListQuery,
  UserRoleCode,
  UserStatus
} from '@/types/user'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const createFormRef = ref()

const filters = reactive<UserListQuery>({
  keyword: '',
  roleCode: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const page = reactive({
  list: [] as UserListItem[],
  total: 0
})

const detail = ref<UserListItem | null>(null)

const createForm = reactive({
  name: '',
  account: '',
  phone: ''
})

const currentRole = computed(() => (authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode ?? 'STUDENT') as UserRoleCode)

const roleOptions = computed(() => {
  const base = [
    { label: 'Admin', value: 'ADMIN' },
    { label: 'Teacher', value: 'TEACHER' },
    { label: 'Student', value: 'STUDENT' }
  ] as const

  switch (currentRole.value) {
    case 'SUPER_ADMIN':
      return [base[0]]
    case 'ADMIN':
      return [base[1]]
    case 'TEACHER':
      return [base[2]]
    default:
      return []
  }
})

const selectedCreateRole = ref<UserRoleCode>(roleOptions.value[0]?.value ?? 'STUDENT')

const filterRoleOptions = computed(() => {
  switch (currentRole.value) {
    case 'SUPER_ADMIN':
      return ['ADMIN', 'TEACHER', 'STUDENT'] as UserRoleCode[]
    case 'ADMIN':
      return ['TEACHER', 'STUDENT'] as UserRoleCode[]
    case 'TEACHER':
      return ['STUDENT'] as UserRoleCode[]
    default:
      return [] as UserRoleCode[]
  }
})

const createRules = {
  name: [{ required: true, message: 'Please enter a name', trigger: 'blur' }],
  account: [{ required: true, message: 'Please enter an account or ID', trigger: 'blur' }]
}

function createFieldLabel(roleCode: UserRoleCode) {
  if (roleCode === 'ADMIN') {
    return 'Account'
  }
  if (roleCode === 'TEACHER') {
    return 'Job number'
  }
  return 'Student number'
}

function createRoleLabel(roleCode: UserRoleCode) {
  if (roleCode === 'SUPER_ADMIN') return 'Super Admin'
  if (roleCode === 'ADMIN') return 'Admin'
  if (roleCode === 'TEACHER') return 'Teacher'
  return 'Student'
}

function resetCreateForm() {
  createForm.name = ''
  createForm.account = ''
  createForm.phone = ''
}

async function loadUsers() {
  loading.value = true

  try {
    const data = await listUsers(filters)
    page.list = data.list
    page.total = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load users')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  void loadUsers()
}

function handleReset() {
  filters.keyword = ''
  filters.roleCode = undefined
  filters.status = undefined
  filters.pageNum = 1
  void loadUsers()
}

async function openDetail(userId: number) {
  detailDialogVisible.value = true
  detailLoading.value = true

  try {
    detail.value = await getUserDetail(userId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load details')
  } finally {
    detailLoading.value = false
  }
}

async function submitCreate() {
  await createFormRef.value?.validate()

  try {
    if (selectedCreateRole.value === 'ADMIN') {
      await createAdmin({
        name: createForm.name,
        account: createForm.account,
        phone: createForm.phone
      } satisfies CreateAdminPayload)
    } else if (selectedCreateRole.value === 'TEACHER') {
      await createTeacher({
        name: createForm.name,
        jobNo: createForm.account,
        phone: createForm.phone
      } satisfies CreateTeacherPayload)
    } else {
      await createStudent({
        name: createForm.name,
        studentNo: createForm.account,
        phone: createForm.phone
      } satisfies CreateStudentPayload)
    }

    ElMessage.success(`${createRoleLabel(selectedCreateRole.value)} created with default password 0000`)
    createDialogVisible.value = false
    resetCreateForm()
    await loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Create failed')
  }
}

async function handleStatusChange(row: UserListItem, status: UserStatus) {
  try {
    await updateUserStatus(row.userId, { status })
    ElMessage.success('User status updated')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Status update failed')
  }
}

async function handleResetPassword(row: UserListItem) {
  try {
    await ElMessageBox.confirm(
      `Reset ${row.name}'s password to 0000 and require password change on next login?`,
      'Reset password',
      {
        confirmButtonText: 'Reset',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )

    await resetPassword(row.userId, { newPassword: '0000' } satisfies ResetPasswordPayload)
    ElMessage.success('Password reset completed')
    await loadUsers()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Password reset failed')
  }
}

async function handleDeleteStudent(row: UserListItem) {
  try {
    await ElMessageBox.confirm(`Delete ${row.name}? This only applies to student accounts.`, 'Delete student', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })

    await deleteStudent(row.userId)
    ElMessage.success('Student deleted')
    await loadUsers()
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Delete failed')
  }
}

function canManageStatus() {
  return currentRole.value === 'SUPER_ADMIN' || currentRole.value === 'ADMIN'
}

function canDeleteStudent(row: UserListItem) {
  return currentRole.value === 'TEACHER' && row.roleCode === 'STUDENT'
}

onMounted(() => {
  if (roleOptions.value.length > 0) {
    selectedCreateRole.value = roleOptions.value[0].value
  }
  void loadUsers()
})
</script>

<template>
  <div class="user-page">
    <section class="hero-card">
      <span class="eyebrow">User Module</span>
      <h2>Manage visible accounts by role</h2>
      <p>
        This module follows the PRD access rules: super admin manages admins, admin manages teachers, and teacher
        manages owned students.
      </p>
    </section>

    <section class="toolbar-card">
      <div class="filter-grid">
        <ElInput v-model="filters.keyword" placeholder="Search by name, account, or login ID" clearable @keyup.enter="handleSearch" />
        <ElSelect v-model="filters.roleCode" placeholder="Role" clearable>
          <ElOption v-for="role in filterRoleOptions" :key="role" :label="createRoleLabel(role)" :value="role" />
        </ElSelect>
        <ElSelect v-model="filters.status" placeholder="Status" clearable>
          <ElOption label="Enabled" value="ENABLED" />
          <ElOption label="Disabled" value="DISABLED" />
        </ElSelect>
      </div>

      <div class="toolbar-actions">
        <ElButton @click="handleReset">Reset</ElButton>
        <ElButton type="primary" @click="handleSearch">Search</ElButton>
        <ElButton v-if="roleOptions.length" type="success" @click="createDialogVisible = true">Create User</ElButton>
      </div>
    </section>

    <section class="table-card">
      <ElTable :data="page.list" v-loading="loading" width="100%">
        <ElTableColumn prop="name" label="Name" min-width="140" />
        <ElTableColumn prop="account" label="Account" min-width="140" />
        <ElTableColumn prop="jobNoOrStudentNo" label="Login ID" min-width="140" />
        <ElTableColumn prop="roleCode" label="Role" min-width="120">
          <template #default="{ row }">
            <ElTag>{{ createRoleLabel(row.roleCode) }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="status" label="Status" min-width="120">
          <template #default="{ row }">
            <ElTag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="creditScore" label="Credit" min-width="100" />
        <ElTableColumn prop="firstLoginRequired" label="First Login" min-width="120">
          <template #default="{ row }">
            <ElTag :type="row.firstLoginRequired ? 'warning' : 'success'">
              {{ row.firstLoginRequired ? 'Required' : 'Done' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="Actions" min-width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <ElButton link type="primary" @click="openDetail(row.userId)">Detail</ElButton>
              <ElButton
                v-if="canManageStatus()"
                link
                type="warning"
                @click="handleStatusChange(row, row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED')"
              >
                {{ row.status === 'ENABLED' ? 'Disable' : 'Enable' }}
              </ElButton>
              <ElButton v-if="canManageStatus()" link type="danger" @click="handleResetPassword(row)">Reset Password</ElButton>
              <ElButton v-if="canDeleteStudent(row)" link type="danger" @click="handleDeleteStudent(row)">Delete</ElButton>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pagination-wrap">
        <ElPagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          background
          layout="total, prev, pager, next"
          :total="page.total"
          @current-change="loadUsers"
        />
      </div>
    </section>

    <ElDialog v-model="createDialogVisible" title="Create User" width="460px" @closed="resetCreateForm">
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <ElFormItem label="Role">
          <ElRadioGroup v-model="selectedCreateRole">
            <ElRadioButton v-for="role in roleOptions" :key="role.value" :label="role.value">
              {{ role.label }}
            </ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="Name" prop="name">
          <ElInput v-model="createForm.name" placeholder="Enter name" />
        </ElFormItem>
        <ElFormItem :label="createFieldLabel(selectedCreateRole)" prop="account">
          <ElInput v-model="createForm.account" :placeholder="`Enter ${createFieldLabel(selectedCreateRole).toLowerCase()}`" />
        </ElFormItem>
        <ElFormItem label="Phone">
          <ElInput v-model="createForm.phone" placeholder="Optional phone number" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitCreate">Create</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="detailDialogVisible" title="User Detail" width="520px">
      <ElSkeleton v-if="detailLoading" :rows="6" animated />
      <div v-else-if="detail" class="detail-grid">
        <article>
          <strong>Name</strong>
          <span>{{ detail.name }}</span>
        </article>
        <article>
          <strong>Account</strong>
          <span>{{ detail.account }}</span>
        </article>
        <article>
          <strong>Login ID</strong>
          <span>{{ detail.jobNoOrStudentNo }}</span>
        </article>
        <article>
          <strong>Role</strong>
          <span>{{ createRoleLabel(detail.roleCode) }}</span>
        </article>
        <article>
          <strong>Status</strong>
          <span>{{ detail.status }}</span>
        </article>
        <article>
          <strong>Phone</strong>
          <span>{{ detail.phone || '--' }}</span>
        </article>
        <article>
          <strong>Credit</strong>
          <span>{{ detail.creditScore }}</span>
        </article>
        <article>
          <strong>First login</strong>
          <span>{{ detail.firstLoginRequired ? 'Password change pending' : 'Completed' }}</span>
        </article>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.user-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.toolbar-card,
.table-card {
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
  color: #155e75;
  background: #cffafe;
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
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

@media (max-width: 960px) {
  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
