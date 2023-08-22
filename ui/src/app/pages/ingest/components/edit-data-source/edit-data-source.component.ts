import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';

@Component({
  selector: 'app-edit-data-source',
  templateUrl: './edit-data-source.component.html',
  styleUrls: ['./edit-data-source.component.scss']
})
export class EditDataSourceComponent {
  @Input() projectId: number;
  @Input() userId: number;
  @Input() folderId: number;
  @Input() sourceName: string;
  @Input() accessType: string;
  @Input() role: string;

  fg: FormGroup;
  public policyData: any = [
    {
      id: 1,
      policyName: "IAM",
    },
    {
      id: 2,
      policyName: "Resource Policy",
    },
    {
      id: 3,
      policyName: "Secret Key / Access Key"
    }
  ];
  constructor(
    public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private fb: FormBuilder,
  ){

  }

  ngOnInit(): void {
    this.fg = this.fb.group({
      sourceName : [{ value: this.sourceName, disabled: true }],
      policyName:[{ value: this.accessType, disabled: true }],
      roleName:[{ value: this.role, disabled: true }]
    })
  }

  public deleteFolder(): void {
    this.modal.close();
  }
}
